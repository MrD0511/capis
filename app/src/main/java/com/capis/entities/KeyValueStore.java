package com.capis.entities;

public class KeyValueStore {
    LRUCache<String, String> cache;

    public KeyValueStore(int capacity) {
        this.cache = new LRUCache<>(capacity);
    }

    public String get(String key) {
        return cache.get(key);
    }

    public void put(String key, String value) {
        cache.put(key, value);
    }
}
