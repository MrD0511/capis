package com.capis.entities;

import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.LinkedHashSet;

public class LRUCache<K, V> {
    private DoublyLinkedList<K, V> list;
    private long capacity;
    private Map<K, Node<K, V>> map;
    private int size = 0;

    static final long NO_EXPIRY = Long.MAX_VALUE;

    public LRUCache(int capacity) {
        this.capacity = capacity;
        this.list = new DoublyLinkedList<>();
        this.map = new HashMap<>();
    }

    public synchronized V get(K key){
        if(!map.containsKey(key)) return null;

        if(System.currentTimeMillis() > map.get(key).expiryAtMillis) {
            Node<K, V> expiredNode = map.get(key);

            this.list.remove(expiredNode);

            map.remove(key);

            size--;

            return null;
        }

        Node<K, V> node = map.get(key);
        this.list.moveToFront(node);
        return node.value;
    }

    public synchronized void put(K key, V value) {
        putInternal(key, value, System.currentTimeMillis() + 24 * 60 * 60 * 1000);
    }

    public synchronized void put(K key, V value, long expiryAtMillis) {
        putInternal(key, value, expiryAtMillis);
    }

    private void putInternal(K key, V value, long expiryAtMillis) {
        Node<K, V> existingNode = map.get(key);
        if(existingNode != null){
            existingNode.value = value;
            existingNode.expiryAtMillis = expiryAtMillis;
            this.list.moveToFront(existingNode);
        }else{
            while(map.size() >= capacity){
                Node<K, V> lruNode = this.list.removeFromEnd();
                if (lruNode != null) {
                    map.remove(lruNode.key);
                }
            }

            Node<K, V> newNode = new Node<>(key, value, expiryAtMillis);
            this.list.addToFront(newNode);

            map.put(key, newNode);
            size++;
        }
    }

    public synchronized V remove(K key){
        Node<K, V> node = map.get(key);
        if(node != null){
            this.list.remove(node);
            size--;
            map.remove(key);
            return node.value;
        }
        return null;
    }

    public synchronized boolean containsKey(K key) {
        if(map.containsKey(key) && System.currentTimeMillis() <= map.get(key).expiryAtMillis) {
            return true;
        }
        
        return false;
    }

    public synchronized Set<K> keys() {
        Set<K> keys = new LinkedHashSet<>();

        long now = System.currentTimeMillis();

        for (Node<K, V> node : list.nodes()) {
            if (now <= node.expiryAtMillis) {
                keys.add(node.key);
            }
        }

        return keys;
    }

    public synchronized int size() {
        return this.size;
    }

    public synchronized void clear() {
        map.clear();
        list.clear();
    }

    public synchronized boolean expire(K key, long ttlMillis){
        Node<K, V> node = map.get(key);
        if(node == null || System.currentTimeMillis() > node.expiryAtMillis) {
            return false;
        }

        node.expiryAtMillis = System.currentTimeMillis() + ttlMillis;
        list.moveToFront(node);
        return true;
    }

    public synchronized Long ttlMillis(K key){
        Node<K, V> node = map.get(key);
        if(node == null) {
            return null;
        }

        if(node.expiryAtMillis == NO_EXPIRY) {
            return -1L;
        }

        return Math.max(
            0L,
            node.expiryAtMillis - System.currentTimeMillis()
        );
    }

    public synchronized boolean persist(K key){
        Node<K, V> node = map.get(key);
        if(node == null) {
            return false;
        }

        node.expiryAtMillis = NO_EXPIRY;
        list.moveToFront(node);
        return true;
    }
}
