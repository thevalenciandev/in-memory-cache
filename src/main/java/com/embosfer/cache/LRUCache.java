package com.embosfer.cache;

import java.util.LinkedHashMap;
import java.util.Map;

public class LRUCache<K, V> extends LinkedHashMap<K, V> {

    private final int maxSize;

    private LRUCache(int maxSize) {
        this.maxSize = maxSize;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return this.size() > maxSize;
    }

    public static <K, V> LRUCache<K, V> withSize(int size) {
        return new LRUCache<>(size);
    }
}
