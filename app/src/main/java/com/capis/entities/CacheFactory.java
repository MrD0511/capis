package com.capis.entities;

import com.capis.EvictionPolicy;
import com.capis.DataTpes.Core.Value;

public class CacheFactory {
    public Cache<String, Value<?>> createCache(int capacity, EvictionPolicy evictionPolicy, int purgeLimit) {
        switch (evictionPolicy) {
            case EvictionPolicy.LRU:
                return new LRUCache<String, Value<?>>(capacity);
            case EvictionPolicy.LFU:
                return new LFUCache<String, Value<?>>(capacity);
            default:
                throw new IllegalArgumentException("Unsupported eviction policy: " + evictionPolicy);
        }
    }
}
