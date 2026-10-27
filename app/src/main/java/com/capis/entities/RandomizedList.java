package com.capis.entities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RandomizedList<E> {
    private final List<E> list = new ArrayList<>();
    private final Map<E, Integer> indexMap = new HashMap<>();

    public boolean add(E element) {
        if (indexMap.containsKey(element)) {
            return false; // Element already exists
        }
        list.add(element);
        indexMap.put(element, list.size() - 1);
        return true;
    }

    public void remove(E element, int index) {
        int lastIndex = list.size() - 1;

        if(index < lastIndex) {
            E lastElement = list.get(lastIndex);
            list.set(index, lastElement);
            indexMap.put(lastElement, index);
        }

        list.remove(lastIndex);
        indexMap.remove(element);
    }

    public boolean remove(E element){
        Integer index = indexMap.get(element);
        if(index == null) {
            return false;
        }

        int lastIndex = list.size() - 1;

        if(index < lastIndex) {
            E lastElement = list.get(lastIndex);
            list.set(index, lastElement);
            indexMap.put(lastElement, index);
        }

        list.remove(lastIndex);
        indexMap.remove(element);
        return true;    
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }

    public int size() {
        return list.size();
    }

    public void clear() {
        list.clear();
        indexMap.clear();
    }

    public E get(int index) {
        return list.get(index);
    }
}
