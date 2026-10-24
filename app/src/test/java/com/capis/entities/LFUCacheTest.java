package com.capis.entities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LFUCacheTest {

    @Test
    void storesAndReturnsValue() {
        LFUCache<String, String> cache = new LFUCache<>(10);

        cache.put("a", "1");

        assertEquals("1", cache.get("a"));
    }

    @Test
    void returnsNullForMissingKey() {
        LFUCache<String, String> cache = new LFUCache<>(10);

        assertNull(cache.get("missing"));
        assertFalse(cache.containsKey("missing"));
    }

    @Test
    void rejectsNonPositiveCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new LFUCache<String, String>(0));
        assertThrows(IllegalArgumentException.class, () -> new LFUCache<String, String>(-1));
    }

    @Test
    void putExistingKeyUpdatesValueWithoutGrowing() {
        LFUCache<String, String> cache = new LFUCache<>(10);

        cache.put("a", "1");
        cache.put("a", "2");

        assertEquals("2", cache.get("a"));
        assertEquals(1, cache.size());
    }

    @Test
    void evictsLeastFrequentlyUsedWhenFull() {
        LFUCache<String, String> cache = new LFUCache<>(2);

        cache.put("a", "1");
        cache.put("b", "2");
        cache.get("a");
        cache.get("a");    // a is now more frequent than b
        cache.put("c", "3");

        assertTrue(cache.containsKey("a"));
        assertFalse(cache.containsKey("b"));
        assertTrue(cache.containsKey("c"));
    }

    @Test
    void removeReturnsValueAndDropsKey() {
        LFUCache<String, String> cache = new LFUCache<>(10);

        cache.put("a", "1");

        assertEquals("1", cache.remove("a"));
        assertNull(cache.get("a"));
        assertEquals(0, cache.size());
    }

    @Test
    void removeMissingKeyReturnsNull() {
        LFUCache<String, String> cache = new LFUCache<>(10);

        assertNull(cache.remove("missing"));
    }

    @Test
    void clearEmptiesTheCache() {
        LFUCache<String, String> cache = new LFUCache<>(10);

        cache.put("a", "1");
        cache.put("b", "2");
        cache.clear();

        assertEquals(0, cache.size());
        assertFalse(cache.containsKey("a"));
    }

    @Test
    void expiredEntryIsInvisible() {
        LFUCache<String, String> cache = new LFUCache<>(10);

        cache.put("a", "1", System.currentTimeMillis() - 1);

        assertNull(cache.get("a"));
        assertFalse(cache.containsKey("a"));
        assertEquals(0, cache.size());
    }

    @Test
    void ttlMillisIsMinusOneWithoutExpiry() {
        LFUCache<String, String> cache = new LFUCache<>(10);

        cache.put("a", "1");

        assertEquals(-1L, cache.ttlMillis("a"));
    }

    @Test
    void ttlMillisIsNullForMissingKey() {
        LFUCache<String, String> cache = new LFUCache<>(10);

        assertNull(cache.ttlMillis("missing"));
    }

    @Test
    void expireSetsRemainingTtl() {
        LFUCache<String, String> cache = new LFUCache<>(10);

        cache.put("a", "1");

        assertTrue(cache.expire("a", 60_000));

        Long ttl = cache.ttlMillis("a");
        assertTrue(ttl > 0 && ttl <= 60_000);
    }

    @Test
    void expireOnMissingKeyReturnsFalse() {
        LFUCache<String, String> cache = new LFUCache<>(10);

        assertFalse(cache.expire("missing", 60_000));
    }

    @Test
    void persistClearsTtl() {
        LFUCache<String, String> cache = new LFUCache<>(10);

        cache.put("a", "1");
        cache.expire("a", 60_000);

        assertTrue(cache.persist("a"));
        assertEquals(-1L, cache.ttlMillis("a"));
        assertFalse(cache.persist("a"));
    }

    @Test
    void purgeRemovesExpiredEntries() {
        LFUCache<String, String> cache = new LFUCache<>(10, 256);
        long past = System.currentTimeMillis() - 1;
        long future = System.currentTimeMillis() + 60_000;

        cache.put("dead", "1", past);
        cache.put("alive", "2", future);

        cache.purgeExpiredEntries();

        assertTrue(cache.containsKey("alive"));
        assertEquals(1, cache.size());
    }
}
