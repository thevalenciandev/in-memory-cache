package com.embosfer.cache;

public class InMemoryCache<K, V> implements DataSource<K, V> {

    private final DataSource<K, V> delegate;
    private final LRUCache<K, V> lruCache;

    public InMemoryCache(int size, DataSource<K, V> delegate) {
        this.delegate = delegate;
        this.lruCache = LRUCache.withSize(size);
    }

    public V getValueFor(K key) {

        V value = lruCache.get(key);
        if (value != null) {
            return value;
        } else {
            V computedValue = delegate.getValueFor(key);
            lruCache.put(key, computedValue);
            return computedValue;
        }
    }
}
