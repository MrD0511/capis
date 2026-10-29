package com.capis.entities;

import java.util.ArrayDeque;
import java.util.List;

import com.capis.DataTypes.Core.ListValue;
import com.capis.DataTypes.Core.StringValue;
import com.capis.DataTypes.Core.Value;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeyValueStoreTest {

    private final KeyValueStore store = new KeyValueStore(new LRUCache<>(100, 256));

    @Test
    void putGetRemoveRoundTrip() {
        store.put("a", new StringValue("1"));

        assertEquals("1", store.get("a").getValue());
        assertTrue(store.containsKey("a"));

        assertEquals("1", store.remove("a").getValue());
        assertNull(store.get("a"));
        assertFalse(store.containsKey("a"));
    }

    @Test
    void sizeCountsLiveKeysAndClearEmptiesTheStore() {
        store.put("a", new StringValue("1"));
        store.put("b", new StringValue("2"));

        assertEquals(2, store.size());

        store.clear();

        assertEquals(0, store.size());
    }

    @Test
    void incrementCreatesKeyAtOne() {
        Value<?> result = store.increment("counter");

        assertEquals("1", result.getValue());
        assertEquals("1", store.get("counter").getValue());
    }

    @Test
    void incrementAddsOneToExistingValue() {
        store.put("counter", new StringValue("41"));

        Value<?> result = store.increment("counter");

        assertEquals("42", result.getValue());
        assertEquals("42", store.get("counter").getValue());
    }

    @Test
    void incrementRejectsNonNumericValue() {
        store.put("counter", new StringValue("abc"));

        assertTrue(assertThrows(IllegalArgumentException.class, () -> store.increment("counter"))
            instanceof IllegalArgumentException);
    }

    @Test
    void incrementRejectsNonStringValue() {
        store.put("list", new ListValue(new ArrayDeque<>(List.of("x"))));

        assertTrue(assertThrows(IllegalArgumentException.class, () -> store.increment("list"))
            instanceof IllegalArgumentException);
    }

    @Test
    void decrementCreatesKeyAtMinusOne() {
        Value<?> result = store.decrement("counter");

        assertEquals("-1", result.getValue());
    }

    @Test
    void decrementSubtractsOneFromExistingValue() {
        store.put("counter", new StringValue("41"));

        Value<?> result = store.decrement("counter");

        assertEquals("40", result.getValue());
    }

    @Test
    void expireAndTtlMillisAndPersist() {
        store.put("a", new StringValue("1"));

        assertTrue(store.expire("a", 60_000));

        Long ttl = store.ttlMillis("a");
        assertTrue(ttl > 0 && ttl <= 60_000);

        assertTrue(store.persist("a"));
        assertEquals(-1L, store.ttlMillis("a"));
    }

    @Test
    void ttlMillisIsNullForMissingKey() {
        assertNull(store.ttlMillis("missing"));
        assertFalse(store.expire("missing", 60_000));
        assertFalse(store.persist("missing"));
    }

    @Test
    void putWithTtlExpiresTheEntry() {
        store.put("a", new StringValue("1"), -1);

        assertNull(store.get("a"));
        assertEquals(0, store.size());
    }

    @Test
    void purgeExpiredEntriesDelegatesToTheCache() {
        store.put("dead", new StringValue("1"), -1);
        store.put("alive", new StringValue("2"));

        store.purgeExpiredEntries();

        assertTrue(store.containsKey("alive"));
        assertEquals(1, store.size());
    }
}
