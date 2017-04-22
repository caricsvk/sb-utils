package milo.utils.cache;

import java.util.Collection;
import java.util.Map;

public interface Cache<T> {

	Collection<T> getCachedCollection(int seconds, CollectionCreator<T> collectionCreator);
	Map<String, T> getCachedMap(int seconds, MapCreator<T> mapCreator);
	T getCachedObject(int seconds, ObjectCreator<T> objectCreator);

}
