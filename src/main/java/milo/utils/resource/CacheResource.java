package milo.utils.resource;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CacheResource<T> {

	private static final Logger LOG = Logger.getLogger(CacheResource.class.getName());

	private String key;
	private final CachedResponse<T> cache;
	private final Supplier<T> dataSupplier;
	private Function<CachedResponse<T>, Response> processFunction;

	private CacheResource(CachedResponse<T> cache, Supplier<T> dataSupplier) {
		this.cache = cache;
		this.dataSupplier = dataSupplier;
	}

	public static <T> CacheResource<T> getInstance(CachedResponse<T> response, Supplier<T> dataSupplier) {
		return new CacheResource<>(response, dataSupplier);
	}
	public static <T> CacheResource<T> getInstance(
			String key, Map<String, CachedResponse<T>> cacheMap, Supplier<T> dataSupplier
	) {
		cacheMap.putIfAbsent(key, new CachedResponse<>());
		return new CacheResource<>(cacheMap.get(key), dataSupplier).withKey(key);
	}

	public static CacheResource<String> getFileInstance(
			String key, Map<String, CachedFileResponse> cacheMap
	) {
		cacheMap.putIfAbsent(key, new CachedFileResponse(key));
		return new CacheResource<>(cacheMap.get(key), null).withKey(key);
	}

	public CachedResponse<T> getCache() {
		return cache;
	}

	public CacheResource<T> expireInMinutes(int minutes) {
		cache.setExpiration(minutes);
		return this;
	}

	public CacheResource<T> withProcessing(Function<CachedResponse<T>, Response> dataProcessor) {
		this.processFunction = dataProcessor;
		return this;
	}

	public void resolve(AsyncResponse asyncResponse) {
		// there was result successfully fetched within last 20 minutes
		if (cache.isCurrent()) {
			asyncResponse.resume(processResult(processFunction));
			return;
		}
		// fetch result or wait for response in executor thread to release http thread
		new Thread(() -> {
			synchronized (cache) { // fetch and process response only one in time
				// not even sequentially when there are parallel requests (1)
				if (!cache.isCurrent()) {
					long start = System.currentTimeMillis();
					try {
						T response = cache instanceof CachedFileResponse ? this.persistAndGetUrlFile(key) : dataSupplier.get();
						cache.setResultUpdateFetched(response);
						LOG.info("fetching took " + (System.currentTimeMillis() - start) + "ms, key: " + key);
//						cache.setResult(dataSupplier.get());
					} catch (Exception e) {
						// not even sequentially when there are parallel requests (2)
						cache.setResultUpdateFetched(null);
						LOG.info("fetching failed after " + (System.currentTimeMillis() - start) + "ms, key: " + key);
						e.printStackTrace();
					}
				}
			}
			// return result even if it's null / error and wasn't resolved yet
			if (!asyncResponse.isDone()) {
				asyncResponse.resume(processResult(processFunction));
			}
			// reset cache immediately if there is empty result / error to fetch it again soon
			if (cache.getResult() == null) {
				cache.clear();
			}
		}).start();
	}

	private T persistAndGetUrlFile(String urlString) throws IOException {
		StringBuilder stringBuilder = new StringBuilder();
		URL url = new URL(urlString);
		String outputFilename = CachedFileResponse.getFilePath(urlString);
		Files.createDirectories(Paths.get(outputFilename).getParent());
		byte[] buffer = new byte[128 * 1024]; // 128KB

		try (
			InputStream stream = url.openStream();
			FileOutputStream outputStream = new FileOutputStream(outputFilename)
		) {
			int bytesRead;
			// TODO - lower the limit! // keep high for paying users, 5 MB for not logged 20MB for logged? etc.
			int contentSizeLimit = 80*1024*1024; // 80MB
			while ((bytesRead = stream.read(buffer)) != -1) {
				outputStream.write(buffer, 0, bytesRead);
				String stringPart = new String(buffer, 0, bytesRead);
				int contentSize = stringBuilder.length();
				if ((contentSize > contentSizeLimit && stringPart.contains("\n")) || contentSize > contentSizeLimit*1.05) {
					stringBuilder.append(stringPart);
					LOG.log(Level.SEVERE, "file size limit exceeded current: " + contentSize);
					break;
				}
				stringBuilder.append(stringPart);
			}
		}
		return (T) stringBuilder.toString();
	}

	private Object processResult(Function<CachedResponse<T>, Response> process) {
		try {
			return process == null ? cache.getResult() : process.apply(cache);
		} catch (Exception e) {
			e.printStackTrace();
			return Response.serverError().build();
		}
	}

	private CacheResource<T> withKey(String key) {
		this.key = key;
		return this;
	}
}
