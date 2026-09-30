package com.capis.entities;

import com.capis.DataTpes.Core.Value;

public class KeyValueStore {
    private LRUCache<String, Value<?>> cache;

    public KeyValueStore(int capacity) {
        this.cache = new LRUCache<>(capacity);
    }

    public Value<?> get(String key) {
        return cache.get(key);
    }

    public void put(String key, Value<?> value) {
        cache.put(key, value);
    }

    public Value<?> remove(String key) {
        return cache.remove(key);
    }
}
