package com.capis.entities;


public interface Cache<K, V> {
    V get(K key);
    void put(K key, V value);
    void put(K key, V value, long expiryAtMillis);
    V remove(K key);
    boolean containsKey(K key);
    // Set<K> keys();
    int size();
    void clear();
    boolean expire(K key, long ttlMillis);
    Long ttlMillis(K key);
    boolean persist(K key);
    void purgeExpiredEntries();
}