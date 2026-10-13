package com.capis.entities;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;

public class LFUCache<K, V> implements Cache<K, V> {
    private Map<K, Node<K, V>> map;
    private Map<Integer, DoublyLinkedList<K, V>> frequencyMap;
    private int capacity;
    private int minFrequency;
    private Long NO_EXPIRY = -1L;
    private int purgeLimit = 100;
    private SortedSet<Integer> frequencySet = new TreeSet<>();
    private RandomizedList<Node<K, V>> randomizedList = new RandomizedList<>();
    private Random random = new Random();


    public LFUCache(int capacity) {
        if(capacity <= 0) {
            throw new IllegalArgumentException(
                "Capacity must be greater than zero"
            );
        }
        this.capacity = capacity;
        this.map = new HashMap<>();
        this.frequencyMap = new HashMap<>();
    }

    public V get(K key){
        Node<K, V> node = map.get(key);

        if(node == null || removeIfExpired(key, node)) {
            return null;
        }

        // Update the frequency of the node
        updateFreq(node);

        return node.value;
    }

    public void put(K key, V value) {
        putInternal(key, value, NO_EXPIRY);
    }

    public void put(K key, V value, long expiryAtMillis) {
        putInternal(key, value, expiryAtMillis);
    }

    private void putInternal(K key, V value, long expiryAtMillis) {
        if(capacity == 0) {
            return;
        }

        Node<K, V> node = map.get(key);

        if(node != null){
            node.value = value;
            node.expiryAtMillis = expiryAtMillis;
            updateFreq(node);
        }

        else{
            evictNode();

            // Create new node
            Node<K, V> newNode = new Node<>(key, value, expiryAtMillis);

            // Add the new node to the frequency map
            map.put(key, newNode);
            randomizedList.add(newNode);

            // Add the new node to the frequency list for frequency 1
            add(newNode, 1);

            minFrequency = 1;
        }
    }

    private void evictNode() {
        // Evict the least frequently used node
        while(map.size() >= capacity){
            DoublyLinkedList<K, V> minFreqList = frequencyMap.get(minFrequency);

            if(minFreqList == null || minFreqList.isEmpty()) {
                throw new IllegalStateException("No nodes to evict, but cache is full. This should not happen.");
            }

            Node<K, V> nodeToEvict = minFreqList.removeFromEnd();
            map.remove(nodeToEvict.key);
            randomizedList.remove(nodeToEvict);
            
            removeIfBucketEmpty(minFrequency);
        }
    }

    private boolean isExpired(Node<K, V> node, long now) {
        return node.expiryAtMillis != NO_EXPIRY && now >= node.expiryAtMillis;
    }

    private boolean removeIfExpired(K key, Node<K, V> node) {
        if (isExpired(node, System.currentTimeMillis())) {

            remove(node);
            map.remove(key);
            randomizedList.remove(node);

            removeIfBucketEmpty(node.frequency);

            return true;
        }

        return false;
    }

    public V remove(K key){
        Node<K, V> node = map.get(key);

        if(node == null || removeIfExpired(key, node)) {
            return null;
        }

        remove(node);
        map.remove(key);
        randomizedList.remove(node);

        removeIfBucketEmpty(node.frequency);

        return node.value;
    }

    public boolean containsKey(K key){
        Node<K, V> node = map.get(key);
        return node != null && !removeIfExpired(key, node);
    }

    public int size() {
        System.out.println("Size of the cache: " + map.size());
        return map.size();
    }

    public void clear() {
        map.clear();
        frequencyMap.clear();
        frequencySet.clear();
        randomizedList.clear();
        minFrequency = 0;
    }

    public boolean expire(K key, long ttlMillis) {
        Node<K, V> node = map.get(key);

        if(node == null || removeIfExpired(key, node)) {
            return false;
        }

        if (ttlMillis <= 0) {
            // If ttlMillis is zero or negative, remove the entry
            remove(key);

            return true;
        }

        node.expiryAtMillis = System.currentTimeMillis() + ttlMillis;
        return true;
    }
    
    public Long ttlMillis(K key) {
        Node<K, V> node = map.get(key);

        if(node == null || removeIfExpired(key, node)) {
            return null;
        }

        if (node.expiryAtMillis == NO_EXPIRY) {
            return -1L; // No expiry set
        }

        return Math.max(0L, node.expiryAtMillis - System.currentTimeMillis());
    }

    public boolean persist(K key) {
        Node<K, V> node = map.get(key);

        if(node == null || removeIfExpired(key, node)) {
            return false;
        }

        if (node.expiryAtMillis == NO_EXPIRY) {
            return false; // Already persistent
        }

        node.expiryAtMillis = NO_EXPIRY;
        return true;
    }

    public void purgeExpiredEntries() {
        if (map.isEmpty()) {
            return;
        }

        int checkedCount = 0;
        long now = System.currentTimeMillis();

        while(checkedCount < purgeLimit && !map.isEmpty()) {
            int randomIndex = random.nextInt(randomizedList.size());
            Node<K, V> node = randomizedList.get(randomIndex);
            checkedCount++;

            if (isExpired(node, now)) {
                remove(node);
                map.remove(node.key);
                randomizedList.remove(node);
        
                removeIfBucketEmpty(node.frequency);
            }
        }
    }

    public void add(Node<K, V> node, int freq){
        DoublyLinkedList<K, V> list = frequencyMap.computeIfAbsent(freq, key -> new DoublyLinkedList<>());
        list.addToFront(node);
        frequencySet.add(freq);
    }

    public void remove(Node<K, V> node){
        Node<K, V> delPrev = node.prev;
        Node<K, V> delNext = node.next;
        delPrev.next = delNext;
        delNext.prev = delPrev;
    }

    private void updateFreq(Node<K, V> node){
        int oldFreq = node.frequency;

        node.frequency++;

        remove(node);
        if(frequencyMap.get(oldFreq).isEmpty()){
            frequencyMap.remove(oldFreq);
            frequencySet.remove(oldFreq);
            
            if(oldFreq == minFrequency){
                minFrequency++;
            }
        }

        add(node, node.frequency);
    }

    private void removeIfBucketEmpty(int freq) {
        DoublyLinkedList<K, V> list = frequencyMap.get(freq);

        if (list != null && list.isEmpty()) {
            frequencyMap.remove(freq);
            frequencySet.remove(freq);

            if (freq == minFrequency) {
                minFrequency = frequencySet.isEmpty()
                        ? 0
                        : frequencySet.first();
            }
        }
    }
}