package com.embosfer.cache;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class InMemoryCache<K, V> implements DataSource<K, V> {

    private final DataSource<K, V> delegate;
    private final LRUCache<K, Future<V>> lruCache;
    private final Lock readLock;
    private final Lock writeLock;
    private final ExecutorService executorService = Executors.newFixedThreadPool(5); // TODO

    public InMemoryCache(int size, DataSource<K, V> delegate) {
        this.delegate = delegate;
        this.lruCache = LRUCache.withSize(size);
        ReadWriteLock lock = new ReentrantReadWriteLock();
        this.readLock = lock.readLock();
        this.writeLock = lock.writeLock();
    }

    public V getValueFor(K key) throws InterruptedException {

        Future<V> futureValue = cachedValueFor(key);
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

    private Future<V> cacheAndGetFutureFor(K key) {
        writeLock.lock();
        try {
            Future<V> futureValue = executorService.submit(() -> delegate.getValueFor(key));
            lruCache.put(key, futureValue);
            return futureValue;
        } finally {
            writeLock.unlock();
        }
    }

    private Future<V> cachedValueFor(K key) {
        readLock.lock();
        try {
            return lruCache.get(key);
        } finally {
            readLock.unlock();
        }
    }
}
