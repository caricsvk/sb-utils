package milo.utils.resource;

import javax.xml.bind.DatatypeConverter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Logger;

public class ResourceHelper {

	private static final Logger LOG = Logger.getAnonymousLogger();

	public static String hashOrOriginal(String key) {
		try {
			MessageDigest messageDigest = MessageDigest.getInstance("MD5");
			messageDigest.update(key.getBytes());
			byte[] digest = messageDigest.digest();
			return DatatypeConverter.printHexBinary(digest);
		} catch (NoSuchAlgorithmException e) {
			System.out.println("MD5 algorithm not available: " + e.getMessage());
			return key;
		}
	}


	public static <T> T timeCapture(String key, Supplier<T> supplier) {
		long startTime = System.currentTimeMillis();
		try {
//			LOG.info("timeCapture started\n\t key: " + key);
			T result = supplier.get();
			LOG.info("timeCapture took " + (System.currentTimeMillis() - startTime) + "ms, key: " + key);
			return result;
		} catch (Exception e) {
			LOG.info("timeCapture failure took " + (System.currentTimeMillis() - startTime) + "ms, key: " + key);
			throw e;
		}
	}

	public static <T> CompletableFuture<T> getFilteredFuture(List<CompletableFuture<T>> futures, Function<T, Boolean> filter) {
		CompletableFuture<T> result = new CompletableFuture<>();
		futures.forEach(future -> future.thenAccept(value -> {
			if (filter.apply(value)) {
				result.complete(value);
			}
		}));
		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenRun(() -> result.complete(null));
		return result;
	}
}

