package com.capis.entities;

import java.util.Map;
import java.util.HashMap;

public class LRUCache<K, V> {
    private DoublyLinkedList<K, V> list;
    private long capacity;
    private Map<K, Node<K, V>> map;

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
            
        }
    }
}
