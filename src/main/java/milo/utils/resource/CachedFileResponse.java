package milo.utils.resource;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class CachedFileResponse extends CachedResponse<String> {

	private static final Logger LOG = Logger.getLogger(CachedResponse.class.getName());

	private final String fileName;

	public CachedFileResponse(String fileName) {
		super();
		this.fileName = fileName;
	}

	public abstract String getFilePath(String fileName);

	@Override
	public synchronized String getResult() {
		boolean inMemory = result != null;
		long start = System.currentTimeMillis();
		if (inMemory) {
			LOG.info("getting file from memory,\n\tgcfmkey: " + fileName);
			return result;
		}
		try {
			result = loadFile();
			LOG.info("getting file from filesystem took: " + (System.currentTimeMillis() - start) +
					"ms, \n\tgcfmkey: " + fileName);
		} catch (Exception ex) {
			LOG.warning("caught getting file from filesystem took: " + (System.currentTimeMillis() - start) +
					"ms, exception: " + ex.getMessage() +"\n\tgcffmkey: " + fileName);
			clear();
		}
		return result;
	}

	public void releaseResultFromMemory() {
		this.result = null;
	}

	public String getResultWithoutFetch() {
		return result;
	}

	public void persistResult(String urlString) throws IOException {
		long start = System.currentTimeMillis();
		StringBuilder stringBuilder = new StringBuilder();
		URL url = new URL(urlString);
		String outputFilename = getFilePath(urlString);
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
		LOG.info("resolving file took " + (System.currentTimeMillis() - start) + "ms, ftkey: " + urlString);
		setResultUpdateFetched(stringBuilder.toString());
	}


	private String loadFile() throws IOException {
		byte[] fileBytes = Files.readAllBytes(Paths.get(getFilePath(fileName)));
		return new String(fileBytes, StandardCharsets.UTF_8);
	}

}
