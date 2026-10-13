package com.capis.entities;

class Node<K, V> {
    K key;
    V value;
    long expiryAtMillis; // Optional: For time-based eviction, if needed
    Node<K, V> next;
    Node<K, V> prev;
    int frequency = 1;

    Node(K key, V value) { // Default expiry time of 1 hour
        this.key = key;
        this.value = value;
        this.expiryAtMillis = -1L; // No expiry by default
    }

    Node(K key, V value, long expiryAtMillis) {
        this.key = key;
        this.value = value;
        this.expiryAtMillis = expiryAtMillis;
    }

    Node(K key, V value, long expiryAtMillis, int frequency) {
        this.key = key;
        this.value = value;
        this.expiryAtMillis = expiryAtMillis;
        this.frequency = frequency;
    }

    Node(K key, V value, int frequency) {
        this.key = key;
        this.value = value;
        this.expiryAtMillis = -1L; // No expiry by default
        this.frequency = frequency;
    }
}