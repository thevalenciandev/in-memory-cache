package com.embosfer.cache;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class InMemoryCache<K, V> implements DataSource<K, V> {

    private final DataSource<K, V> delegate;
    private final LRUCache<K, Future<V>> lruCache;
    private final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public InMemoryCache(int size, DataSource<K, V> delegate) {
        this.delegate = delegate;
        this.lruCache = LRUCache.withSize(size);
    }

    public V getValueFor(K key) throws InterruptedException {

        Future<V> futureValue = cachedFutureFor(key);
        if (futureValue == null) {
            return valueOrBlowUp(cacheAndGetFutureFor(key), key);
        } else {
            return valueOrBlowUp(futureValue, key);
        }
    }

    private V valueOrBlowUp(Future<V> futureValue, K key) throws InterruptedException {
        try {
            return futureValue.get();
        } catch (ExecutionException e) {
            // TODO: log error
            throw new RuntimeException("Something went wrong when computing value for " + key + ". " + e.getMessage());
        }
    }

    private synchronized Future<V> cacheAndGetFutureFor(K key) {
        if (lruCache.containsKey(key)) { // re-check as another thread might have got the write lock before we did
            return lruCache.get(key);
        }
        Future<V> futureValue = executorService.submit(() -> delegate.getValueFor(key));
        lruCache.put(key, futureValue);
        return futureValue;
    }

    private synchronized Future<V> cachedFutureFor(K key) {
        return lruCache.get(key);
    }
}
