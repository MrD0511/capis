package com.capis.entities;

import java.util.Set;

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

    public void put(String key, Value<?> value, long ttlMillis) {
        cache.put(key, value, System.currentTimeMillis() + ttlMillis);
    }

    public boolean expire(String key, long ttlMillis) { return cache.expire(key, ttlMillis); }
    public Long ttlMillis(String key)                 { return cache.ttlMillis(key); }
    public boolean persist(String key)                { return cache.persist(key); }
    public Set<String> keys()                         { return cache.keys(); }
    public int size()                                 { return cache.size(); }
    public void clear()                               { cache.clear(); }

    public Value<?> increment(String key){
        Value<?> value = cache.get(key);

        if (value == null) {
            StringValue newValue = new StringValue("1");
            cache.put(key, newValue);
            return newValue;
        }

        if (!(value instanceof StringValue stringValue)) {
            throw new IllegalArgumentException("Value is not a string");
        }

        long val;
        
        try {
            val = Long.parseLong(stringValue.getValue());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Value is not a valid integer");
        }

        val++;

        stringValue.setValue(String.valueOf(val));

        return stringValue;
    }

    public Value<?> decrement(String key){
        Value<?> value = cache.get(key);

        if (value == null) {
            StringValue newValue = new StringValue("-1");
            cache.put(key, newValue);
            return newValue;
        }

        if (!(value instanceof StringValue stringValue)) {
            throw new IllegalArgumentException("Value is not a string");
        }

        long val;
        
        try {
            val = Long.parseLong(stringValue.getValue());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Value is not a valid integer");
        }

        val--;

        stringValue.setValue(String.valueOf(val));

        return stringValue;
    }


}
