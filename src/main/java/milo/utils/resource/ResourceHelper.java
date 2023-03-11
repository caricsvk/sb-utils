package milo.utils.resource;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;
import java.time.LocalDateTime;
import java.util.function.Function;
import java.util.function.Supplier;

// TODO move to shared/util library
public class ResourceHelper {

	public static <T> void resumeWithCachedData(CachedResponse<T> response, AsyncResponse asyncResponse, Supplier<T> getData) {
		processWithCachedData(response, asyncResponse, getData, null);
	}

	public static <T> void processWithCachedData(CachedResponse<T> response, AsyncResponse asyncResponse,
										   Supplier<T> getDataSupplier, Function<T, Response> processFn) {
		// there was result successfully fetched within last 20 minutes
		if (response.isCurrent()) {
			asyncResponse.resume(processResult(processFn, response.result));
			return;
		}
		// fetch result or wait for response in executor thread to release http thread
		new Thread(() -> {
			synchronized (response) { // fetch and process response only one in time
				// not even sequentially when there are parallel requests (1)
				if (!response.isCurrent()) {
					try {
						response.result = getDataSupplier.get();
						response.fetched = LocalDateTime.now();
					} catch (Exception e) {
						response.result = null;
						response.fetched = LocalDateTime.now(); // not even sequentially when there are parallel requests (2)
						e.printStackTrace();
					}
				}
			}
			// return result even if it's null / error and wasn't resolved yet
			if (!asyncResponse.isDone()) {
				asyncResponse.resume(processResult(processFn, response.result));
			}
			// reset cache immediately if there is empty result / error to fetch it again soon
			if (response.result == null) {
				response.fetched = null;
			}
		}).start();
	}

	private static <T> Object processResult(Function<T, Response> process, T result) {
		try {
			return process == null ? result : process.apply(result);
		} catch (Exception e) {
			e.printStackTrace();
			return Response.serverError().build();
		}
	}

}

