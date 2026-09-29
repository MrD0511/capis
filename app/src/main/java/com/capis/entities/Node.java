package com.capis.entities;

class Node<K, V> {
    K key;
    V value;
    long expiryAtMillis; // Optional: For time-based eviction, if needed
    Node<K, V> next;
    Node<K, V> prev;

    Node(K key, V value) { // Default expiry time of 1 hour
        this.key = key;
        this.value = value;
        this.expiryAtMillis = System.currentTimeMillis() + 24 * 60 * 60 * 1000; // 1 hour
    }

    Node(K key, V value, long expiryAtMillis) {
        this.key = key;
        this.value = value;
        this.expiryAtMillis = expiryAtMillis;
    }
}