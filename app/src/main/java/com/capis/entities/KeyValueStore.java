package com.capis.entities;

import com.capis.DataTpes.Core.StringValue;
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

    public boolean containsKey(String key) {
        return cache.containsKey(key);
    }

    public synchronized Value<?> increment(String key){
        Value<?> value = cache.get(key);

        if (value == null) {
            return null;
        }

        if (!(value instanceof StringValue stringValue)) {
            throw new IllegalArgumentException("Value is not a string");
        }

        long val;
        
        try {
            val = Long.parseLong(stringValue.getValue());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                "Value is not an integer"
            );
        }

        val++;

        stringValue.setValue(String.valueOf(val));

        return stringValue;
    }
}
