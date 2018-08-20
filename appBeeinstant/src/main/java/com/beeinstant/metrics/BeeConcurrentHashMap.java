package com.beeinstant.metrics;

import java.util.concurrent.ConcurrentHashMap;

public class BeeConcurrentHashMap<K, V> extends ConcurrentHashMap<K, V> {
    public V putIfAbsent(K key, V value) {
        if (!this.containsKey(key))
            return this.put(key, value);
        else
            return this.get(key);
    }
}
