package milo.utils.cache;

public interface ObjectCreator<T> extends Creator {
	T create();
}
