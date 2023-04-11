package milo.utils.resource;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CacheResource<T> {

	private static final Logger LOG = Logger.getLogger(CacheResource.class.getName());

	private String key;
	private final CachedResponse<T> cache;
	private final Supplier<T> dataSupplier;
	private Function<CachedResponse<T>, Boolean> preProcess = cache -> true;
	private Function<CachedResponse<T>, Response> process;
	private Consumer<CachedResponse<T>> postProcess;

	private CacheResource(CachedResponse<T> cache, Supplier<T> dataSupplier) {
		this.cache = cache;
		this.dataSupplier = dataSupplier;
	}

	public static <T> CacheResource<T> getInstance(CachedResponse<T> response, Supplier<T> dataSupplier) {
		return new CacheResource<>(response, dataSupplier);
	}

	public static <T> CacheResource<T> getInstance(String key, CachedResponse<T> response, Supplier<T> dataSupplier) {
		return new CacheResource<>(response, dataSupplier).withKey(key);
	}

	public static <T> CacheResource<T> getInstance(
			String key, Map<String, CachedResponse<T>> cacheMap, Supplier<T> dataSupplier
	) {
		cacheMap.putIfAbsent(key, new CachedResponse<>());
		return new CacheResource<>(cacheMap.get(key), dataSupplier).withKey(key);
	}

	public CachedResponse<T> getCache() {
		return cache;
	}

	public CacheResource<T> expireInMinutes(int minutes) {
		cache.setExpiration(minutes);
		return this;
	}

	public CacheResource<T> preProcess(Function<CachedResponse<T>, Boolean> preProcess) {
		this.preProcess = preProcess;
		return this;
	}

	public CacheResource<T> process(Function<CachedResponse<T>, Response> dataProcessor) {
		this.process = dataProcessor;
		return this;
	}

	public CacheResource<T> postProcess(Consumer<CachedResponse<T>> postProcess) {
		this.postProcess = postProcess;
		return this;
	}

	public void resolve(AsyncResponse asyncResponse) {
		// there was result successfully fetched within last N minutes
		if (cache.isCurrent()) {
			asyncResponse.resume(processResult(process));
			return;
		}
		// fetch result or wait for response in executor thread to release http thread
		CompletableFuture.runAsync(() -> {
			synchronized (cache) { // fetch and process response only one in time
				// not even sequentially when there are parallel requests (1)
				if (preProcess.apply(cache) && !cache.isCurrent()) {
					setupCache();
				}
			}
			// return result even if it's null / error and wasn't resolved yet
			if (!asyncResponse.isDone()) {
				asyncResponse.resume(processResult(process));
			}
			synchronized (cache) {
				// reset cache immediately if there is empty result / error to fetch it again soon
				if (cache.getResult() == null) {
					cache.clear();
				} else if (postProcess != null) {
					postProcess.accept(cache);
				}
			}
		});
	}

	private void setupCache() {
		long start = System.currentTimeMillis();
		try {
			if (dataSupplier != null) {
				cache.setResultUpdateFetched(dataSupplier.get());
			}
			LOG.info("resolving took " + (System.currentTimeMillis() - start) + "ms, ftkey: " + key);
		} catch (Exception e) {
			// not even sequentially when there are parallel requests (2)
			cache.setResultUpdateFetched(null);
			LOG.log(Level.WARNING, "resolving failed after " + (System.currentTimeMillis() - start) + "ms, ffkey: " + key +
					";\n\tresolving caught ex: " + e.getMessage(), e);
		}
	}

	private Object processResult(Function<CachedResponse<T>, Response> process) {
		long start = System.currentTimeMillis();
		try {
			Object result = process == null ? cache.getResult() : process.apply(cache);
			LOG.info("processing took " + (System.currentTimeMillis() - start) + "ms, ftkey: " + key);
			return result;
		} catch (Exception e) {
			LOG.log(Level.WARNING, "caught processResult exception (& clearing cache), took " +
							(System.currentTimeMillis() - start) + "ms, key: " + key + ", " + e.getMessage());
			cache.clear();
			return Response.serverError().build();
		}
	}

	private CacheResource<T> withKey(String key) {
		this.key = key;
		return this;
	}
}
