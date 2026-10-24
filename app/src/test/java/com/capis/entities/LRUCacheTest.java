package com.capis.entities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LRUCacheTest {

    @Test
    void storesAndReturnsValue() {
        LRUCache<String, String> cache = new LRUCache<>(10);

        cache.put("a", "1");

        assertEquals("1", cache.get("a"));
    }

    @Test
    void returnsNullForMissingKey() {
        LRUCache<String, String> cache = new LRUCache<>(10);

        assertNull(cache.get("missing"));
        assertFalse(cache.containsKey("missing"));
    }

    @Test
    void rejectsNonPositiveCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new LRUCache<String, String>(0));
        assertThrows(IllegalArgumentException.class, () -> new LRUCache<String, String>(-1));
    }

    @Test
    void putExistingKeyUpdatesValueWithoutGrowing() {
        LRUCache<String, String> cache = new LRUCache<>(10);

        cache.put("a", "1");
        cache.put("a", "2");

        assertEquals("2", cache.get("a"));
        assertEquals(1, cache.size());
    }

    @Test
    void evictsLeastRecentlyUsedWhenFull() {
        LRUCache<String, String> cache = new LRUCache<>(2);

        cache.put("a", "1");
        cache.put("b", "2");
        cache.get("a");      // a is now more recent than b
        cache.put("c", "3"); // evicts b

        assertTrue(cache.containsKey("a"));
        assertFalse(cache.containsKey("b"));
        assertTrue(cache.containsKey("c"));
        assertEquals(2, cache.size());
    }

    @Test
    void removeReturnsValueAndDropsKey() {
        LRUCache<String, String> cache = new LRUCache<>(10);

        cache.put("a", "1");

        assertEquals("1", cache.remove("a"));
        assertNull(cache.get("a"));
        assertEquals(0, cache.size());
    }

    @Test
    void removeMissingKeyReturnsNull() {
        LRUCache<String, String> cache = new LRUCache<>(10);

        assertNull(cache.remove("missing"));
    }

    @Test
    void clearEmptiesTheCache() {
        LRUCache<String, String> cache = new LRUCache<>(10);

        cache.put("a", "1");
        cache.put("b", "2");
        cache.clear();

        assertEquals(0, cache.size());
        assertFalse(cache.containsKey("a"));
    }

    @Test
    void expiredEntryIsInvisible() {
        LRUCache<String, String> cache = new LRUCache<>(10);

        cache.put("a", "1", System.currentTimeMillis() - 1);

        assertNull(cache.get("a"));
        assertFalse(cache.containsKey("a"));
        assertEquals(0, cache.size());
    }

    @Test
    void ttlMillisIsMinusOneWithoutExpiry() {
        LRUCache<String, String> cache = new LRUCache<>(10);

        cache.put("a", "1");

        assertEquals(-1L, cache.ttlMillis("a"));
    }

    @Test
    void ttlMillisIsNullForMissingKey() {
        LRUCache<String, String> cache = new LRUCache<>(10);

        assertNull(cache.ttlMillis("missing"));
    }

    @Test
    void expireSetsRemainingTtl() {
        LRUCache<String, String> cache = new LRUCache<>(10);

        cache.put("a", "1");

        assertTrue(cache.expire("a", 60_000));

        Long ttl = cache.ttlMillis("a");
        assertTrue(ttl > 0 && ttl <= 60_000);
    }

    @Test
    void expireOnMissingKeyReturnsFalse() {
        LRUCache<String, String> cache = new LRUCache<>(10);

        assertFalse(cache.expire("missing", 60_000));
    }

    @Test
    void expireWithZeroRemovesTheKey() {
        LRUCache<String, String> cache = new LRUCache<>(10);

        cache.put("a", "1");

        assertTrue(cache.expire("a", 0));
        assertFalse(cache.containsKey("a"));
    }

    @Test
    void persistClearsTtl() {
        LRUCache<String, String> cache = new LRUCache<>(10);

        cache.put("a", "1");
        cache.expire("a", 60_000);

        assertTrue(cache.persist("a"));
        assertEquals(-1L, cache.ttlMillis("a"));
        assertFalse(cache.persist("a"));
    }

    @Test
    void purgeRemovesOnlyExpiredEntries() {
        LRUCache<String, String> cache = new LRUCache<>(10, 256);
        long past = System.currentTimeMillis() - 1;
        long future = System.currentTimeMillis() + 60_000;

        cache.put("dead1", "1", past);
        cache.put("dead2", "2", past);
        cache.put("alive1", "3", future);
        cache.put("alive2", "4");

        cache.purgeExpiredEntries();

        assertFalse(cache.containsKey("dead1"));
        assertFalse(cache.containsKey("dead2"));
        assertTrue(cache.containsKey("alive1"));
        assertTrue(cache.containsKey("alive2"));
        assertEquals(2, cache.size());
    }

    @Test
    void repeatedPurgesEventuallyClearAllExpiredEntries() {
        LRUCache<String, String> cache = new LRUCache<>(10, 1);
        long past = System.currentTimeMillis() - 1;

        for (int i = 0; i < 5; i++) {
            cache.put("k" + i, "v", past);
        }

        for (int pass = 0; pass < 5; pass++) {
            cache.purgeExpiredEntries();
        }

        assertEquals(0, cache.size());
    }
}
