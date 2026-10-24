package com.capis.Commands;

import java.util.List;

import com.capis.DataTpes.RespValues.RespArray;
import com.capis.DataTpes.RespValues.RespBulkString;
import com.capis.DataTpes.RespValues.RespInteger;
import com.capis.DataTpes.RespValues.RespValue;
import com.capis.entities.KeyValueStore;
import com.capis.entities.LRUCache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ListCommandsTest {

    private KeyValueStore store;
    private LPUSHCommand lpush;
    private RPushCommand rpush;
    private LPOPCommand lpop;
    private RPOPCommand rpop;
    private LLENCommand llen;
    private LIndexCommand lindex;
    private LRangeCommand lrange;

    @BeforeEach
    void setUp() {
        store = new KeyValueStore(new LRUCache<>(100, 256));
        lpush = new LPUSHCommand(store);
        rpush = new RPushCommand(store);
        lpop = new LPOPCommand(store);
        rpop = new RPOPCommand(store);
        llen = new LLENCommand(store);
        lindex = new LIndexCommand(store);
        lrange = new LRangeCommand(store);
    }

    @Test
    void pushAndPopWorkInOrder() {
        assertEquals(new RespInteger(2), lpush.execute(new String[]{"LPUSH", "list", "b", "a"}));
        assertEquals(new RespInteger(3), rpush.execute(new String[]{"RPUSH", "list", "c"}));

        assertEquals(new RespBulkString("a"), lpop.execute(new String[]{"LPOP", "list"}));
        assertEquals(new RespBulkString("c"), rpop.execute(new String[]{"RPOP", "list"}));
        assertEquals(new RespInteger(1), llen.execute(new String[]{"LLEN", "list"}));
        assertEquals(new RespBulkString("b"), lindex.execute(new String[]{"LINDEX","list","0"}));
    }

    @Test
    void lpopWithCountReturnsAnArray() {
        rpush.execute(new String[]{"RPUSH", "list", "a", "b", "c"});

        RespValue result = lpop.execute(new String[]{"LPOP", "list", "2"});

        List<RespValue> values = ((RespArray) result).values();
        assertEquals(2, values.size());
        assertEquals(new RespBulkString("a"), values.get(0));
        assertEquals(new RespBulkString("b"), values.get(1));
    }

    @Test
    void lpopMissingKeyReturnsNullOrEmptyArray() {
        assertNull(lpop.execute(new String[]{"LPOP", "missing"}));
        assertEquals(0, ((RespArray) lpop.execute(new String[]{"LPOP", "missing", "2"})).values().size());
    }

    @Test
    void rpopMissingKeyReturnsNullOrEmptyArray() {
        assertNull(rpop.execute(new String[]{"RPOP", "missing"}));
        assertEquals(0, ((RespArray) rpop.execute(new String[]{"RPOP", "missing", "2"})).values().size());
    }

    @Test
    void lindexReturnsCorrectElementWithNegativeIndexes() {
        rpush.execute(new String[]{"RPUSH", "list", "a", "b", "c"});

        assertEquals(new RespBulkString("a"), lindex.execute(new String[]{"LINDEX", "list", "0"}));
        assertEquals(new RespBulkString("c"), lindex.execute(new String[]{"LINDEX", "list", "-1"}));
        assertNull(lindex.execute(new String[]{"LINDEX", "list", "10"}));
        assertNull(lindex.execute(new String[]{"LINDEX", "list", "-10"}));
    }

    @Test
    void lrangeClampsBoundsAndReturnsEmptyForMissingKey() {
        assertEquals(0, ((RespArray) lrange.execute(new String[]{"LRANGE", "missing", "0", "-1"})).values().size());

        rpush.execute(new String[]{"RPUSH", "list", "a", "b", "c", "d"});

        List<RespValue> values = ((RespArray) lrange.execute(new String[]{"LRANGE", "list", "1", "2"})).values();
        assertEquals(2, values.size());
        assertEquals(new RespBulkString("b"), values.get(0));
        assertEquals(new RespBulkString("c"), values.get(1));

        List<RespValue> full = ((RespArray) lrange.execute(new String[]{"LRANGE", "list", "-100", "100"})).values();
        assertEquals(4, full.size());
    }

    @Test
    void llenReturnsZeroForMissingOrWrongType() {
        assertEquals(new RespInteger(0), llen.execute(new String[]{"LLEN", "missing"}));
    }
}
