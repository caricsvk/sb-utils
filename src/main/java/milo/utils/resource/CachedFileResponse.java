package milo.utils.resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Logger;

public class CachedFileResponse extends CachedResponse<String> {

	private static final Logger LOG = Logger.getLogger(CachedResponse.class.getName());

	private final String fileName;

	public CachedFileResponse(String fileName) {
		super();
		this.fileName = fileName;
	}

	@Override
	public synchronized String getResult() {
		boolean inMemory = result != null;
		LOG.info("getting cache from filesystem, already in memory: " + inMemory + ", key: " + fileName);
		if (inMemory) {
			return result;
		}
		try {
			result = loadFile();
		} catch (Exception ex) {
			LOG.warning("caught getting file cache from file: " + ex.getMessage());
			clear();
		}
		return result;
	}

	public void releaseResultFromMemory() {
		this.result = null;
	}

	private String loadFile() throws IOException {
		byte[] fileBytes = Files.readAllBytes(Paths.get(getFilePath(fileName)));
		return new String(fileBytes, StandardCharsets.UTF_8);
	}

	public static String getFilePath(String fileName) {
		return ResourceHelper.getCacheDirectory() + "/" + getFileNameHash(fileName);
	}

	public static String getFileNameHash(String fileName) {
		return ResourceHelper.hashOrOriginal(fileName);
	}
}
