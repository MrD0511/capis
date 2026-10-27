package com.capis.entities;


import com.capis.DataTpes.Core.Value;
import com.capis.DataTypes.EvictionPolicy;

public class CacheFactory {
    public Cache<String, Value<?>> createCache(int capacity, EvictionPolicy evictionPolicy, int purgeLimit) {
        return switch (evictionPolicy) {
            case LRU -> new LRUCache<>(capacity, purgeLimit);
            case LFU -> new LFUCache<>(capacity, purgeLimit);
            default -> throw new IllegalArgumentException("Unsupported eviction policy: " + evictionPolicy);
        };
    }
}
