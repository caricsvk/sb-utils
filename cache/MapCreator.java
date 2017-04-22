package milo.utils.cache;

import java.util.Map;

public interface MapCreator<T> extends Creator {

	Map<String, T> create();

}
