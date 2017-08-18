package milo.utils.cache;

import java.util.Collection;

public interface CollectionCreator<T> extends Creator {
	Collection<T> create();
}
