package com.embosfer.cache;

public interface DataSource<K, V> {

    V getValueFor(K key) throws Exception;

}
