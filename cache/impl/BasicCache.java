package milo.utils.cache.impl;

import milo.utils.cache.Cache;
import milo.utils.cache.CollectionCreator;
import milo.utils.cache.Creator;
import milo.utils.cache.MapCreator;
import milo.utils.cache.ObjectCreator;
import org.springframework.context.annotation.Bean;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class BasicCache<T> implements Cache<T> {

	private static final Logger LOG = Logger.getLogger(BasicCache.class.getName());
	private static final Map<String, CacheHolder> cacheHolders = new ConcurrentHashMap<>();

	@Override
	public Collection<T> getCachedCollection(int seconds, CollectionCreator<T> collectionCreator) {
		String key = collectionCreator.getClass().toString() + "-collection";
		return (Collection) getCacheHolder(key, seconds, collectionCreator).getCache();
	}

	@Override
	public Map<String, T> getCachedMap(int seconds, MapCreator<T> mapCreator) {
		String key = mapCreator.getClass().toString() + "-map";
		return (Map) getCacheHolder(key, seconds, mapCreator).getCache();
	}

	@Override
	public T getCachedObject(int seconds, ObjectCreator<T> objectCreator) {
		String key = objectCreator.getClass().toString() + "-object";
		return (T) getCacheHolder(key, seconds, objectCreator).getCache();
	}


	private CacheHolder getCacheHolder(String key, int seconds, Creator creator) {
		CacheHolder cacheHolder = cacheHolders.get(key);
		if (cacheHolder == null || System.currentTimeMillis() - cacheHolder.getCreated() > 1000*seconds) {
			LOG.info("###### ===== BasicCache.creatingCache for " + key);
			cacheHolder = new CacheHolder(creator.create());
			cacheHolders.put(key, cacheHolder);
		}
		return cacheHolder;
	}

	private static class CacheHolder {
		private long created;
		private Object cache;

		public CacheHolder(Object cache) {
			created = System.currentTimeMillis();
			this.cache = cache;
		}

		public Object getCache() {
			return cache;
		}

		public void setCache(Object cache) {
			this.cache = cache;
		}

		public long getCreated() {
			return created;
		}
	}
}
