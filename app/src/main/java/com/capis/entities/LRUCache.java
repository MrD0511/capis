package com.capis.entities;

import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.HashMap;
import java.util.LinkedHashSet;

public class LRUCache<K, V> implements Cache<K, V> {
    private DoublyLinkedList<K, V> list;
    private long capacity;
    private Map<K, Node<K, V>> map;
    private int purgeLimit = 100;
    private RandomizedList<Node<K, V>> randomList;
    private Random random;

    static final long NO_EXPIRY = -1L;

    public LRUCache(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException(
                "Capacity must be greater than zero"
            );
        }
        this.capacity = capacity;
        this.list = new DoublyLinkedList<>();
        this.map = new HashMap<>();
        this.random = new Random();
        this.randomList = new RandomizedList<>();
    }

    public LRUCache(int capacity, int purgeLimit) {
        this(capacity);
        if (purgeLimit <= 0) {
            throw new IllegalArgumentException(
                "Purge limit must be greater than zero"
            );
        }
        this.purgeLimit = purgeLimit;
    }

    public V get(K key){
        Node<K, V> node = map.get(key);
        
        if(node == null || removeIfExpired(node)) {
            return null;
        }

        this.list.moveToFront(node);
        return node.value;
    }

    public void put(K key, V value) {
        putInternal(key, value, NO_EXPIRY);
    }

    public void put(K key, V value, long expiryAtMillis) {
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

            randomList.add(newNode);

            map.put(key, newNode);
        }
    }

    public V remove(K key){
        Node<K, V> node = map.get(key);

        if(node == null || removeIfExpired(node)) {
            return null;
        }

        this.list.remove(node);
        map.remove(key);
        randomList.remove(node);

        return node.value;
    }

    public boolean containsKey(K key) {
        Node<K, V> node = map.get(key);

        return node != null && !removeIfExpired(node);
    }

    public Set<K> keys() {
        Set<K> keys = new LinkedHashSet<>();

        for (Node<K, V> node : list.nodes()) {
            if (!removeIfExpired(node)) {
                keys.add(node.key);
            }
        }

        return keys;
    }

    public int size() {
        purgeExpiredEntries(); 
        return this.map.size();
    }

    public void clear() {
        map.clear();
        list.clear();
        randomList.clear();
    }

    public boolean expire(K key, long ttlMillis){
        Node<K, V> node = map.get(key);
        
        if(node == null || removeIfExpired(node)) {
            return false;
        }

        if (ttlMillis <= 0) {
            remove(key);
            return true;
        }

        node.expiryAtMillis = System.currentTimeMillis() + ttlMillis;
        return true;
    }

    public Long ttlMillis(K key){
        Node<K, V> node = map.get(key);
        
        if(node == null || removeIfExpired(node)) {
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

    public boolean persist(K key){
        Node<K, V> node = map.get(key);
        if(node == null || removeIfExpired(node)) {
            return false;
        }

        if(node.expiryAtMillis == NO_EXPIRY) {
            return false;
        }

        node.expiryAtMillis = NO_EXPIRY;
        return true;
    }

    private boolean isExpired(Node<K, V> node, long now) {
        return node.expiryAtMillis != NO_EXPIRY && now >= node.expiryAtMillis;
    }

    private boolean removeIfExpired(Node<K, V> node) {
        if (isExpired(node, System.currentTimeMillis())) {
            this.list.remove(node);
            map.remove(node.key);
            randomList.remove(node);

            return true;
        }

        return false;
    }

    public void purgeExpiredEntries() {
        if (map.isEmpty()) {
            return;
        }

        int checkedCount = 0;
        long now = System.currentTimeMillis();

        while(checkedCount < purgeLimit && !randomList.isEmpty()) {
            int randomIndex = random.nextInt(randomList.size());
            Node<K, V> node = randomList.get(randomIndex);
            checkedCount++;

            if (isExpired(node, now)) {
                this.list.remove(node);
                map.remove(node.key);
                randomList.remove(node);
            }
        }
    }
}
