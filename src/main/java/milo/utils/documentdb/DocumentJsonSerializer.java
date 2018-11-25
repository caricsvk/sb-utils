package milo.utils.documentdb;

import java.io.IOException;

public interface DocumentJsonSerializer {
	String serialize(Object object) throws IOException;
	<T>T deserialize(String jsonString, Class<T> classType) throws IOException;
}
