package com.thevalenciandev.cache;

public interface DataSource<K, V> {

    V getValueFor(K key) ;

}
