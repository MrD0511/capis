package com.capis.entities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RandomizedListTest {

    @Test
    void addsElementsAndTracksSize() {
        RandomizedList<String> list = new RandomizedList<>();

        assertTrue(list.isEmpty());
        assertTrue(list.add("a"));
        assertTrue(list.add("b"));

        assertFalse(list.isEmpty());
        assertEquals(2, list.size());
    }

    @Test
    void rejectsDuplicateElements() {
        RandomizedList<String> list = new RandomizedList<>();

        assertTrue(list.add("a"));
        assertFalse(list.add("a"));
        assertEquals(1, list.size());
    }

    @Test
    void getReturnsElementsByIndex() {
        RandomizedList<String> list = new RandomizedList<>();

        list.add("a");
        list.add("b");

        assertEquals("a", list.get(0));
        assertEquals("b", list.get(1));
    }

    @Test
    void removeSwapsTheLastElementIntoTheRemovedSlot() {
        RandomizedList<String> list = new RandomizedList<>();

        list.add("a");
        list.add("b");
        list.add("c");

        assertTrue(list.remove("a"));

        assertEquals(2, list.size());
        assertEquals("c", list.get(0));
        assertEquals("b", list.get(1));
    }

    @Test
    void removeIsIdempotentForMissingElements() {
        RandomizedList<String> list = new RandomizedList<>();

        list.add("a");

        assertFalse(list.remove("b"));
        assertEquals(1, list.size());
    }

    @Test
    void removingTheLastElementLeavesTheOthersInPlace() {
        RandomizedList<String> list = new RandomizedList<>();

        list.add("a");
        list.add("b");

        assertTrue(list.remove("b"));

        assertEquals(1, list.size());
        assertEquals("a", list.get(0));
    }

    @Test
    void clearEmptiesTheListAndAllowsRepeats() {
        RandomizedList<String> list = new RandomizedList<>();

        list.add("a");
        list.clear();

        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
        assertTrue(list.add("a"));
    }
}
