package com.capis.Commands;

import java.util.ArrayDeque;
import java.util.List;

import com.capis.DataTypes.Core.ListValue;
import com.capis.DataTypes.RespValues.RespArray;
import com.capis.DataTypes.RespValues.RespBulkString;
import com.capis.DataTypes.RespValues.RespErr;
import com.capis.DataTypes.RespValues.RespInteger;
import com.capis.DataTypes.RespValues.RespSimpleString;
import com.capis.DataTypes.RespValues.RespValue;
import com.capis.entities.KeyValueStore;
import com.capis.entities.LRUCache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StringCommandsTest {

    private KeyValueStore store;
    private SetCommand set;
    private GetCommand get;
    private MSetCommand mset;
    private MGetCommand mget;
    private AppendCommand append;
    private STRLENCommand strlen;
    private GETDELCommand getdel;
    private INCRCommand incr;
    private DECRCommand decr;
    private INCRBYFLOATCommand incrByFloat;
    private SETEXCommand setex;
    private TTLCommand ttl;

    @BeforeEach
    void setUp() {
        store = new KeyValueStore(new LRUCache<>(100, 256));
        set = new SetCommand(store);
        get = new GetCommand(store);
        mset = new MSetCommand(store);
        mget = new MGetCommand(store);
        append = new AppendCommand(store);
        strlen = new STRLENCommand(store);
        getdel = new GETDELCommand(store);
        incr = new INCRCommand(store);
        decr = new DECRCommand(store);
        incrByFloat = new INCRBYFLOATCommand(store);
        setex = new SETEXCommand(store);
        ttl = new TTLCommand(store);
    }

    @Test
    void setStoresAndGetReturns() {
        assertEquals(new RespSimpleString("OK"), set.execute(new String[]{"SET", "k", "v"}));
        assertEquals(new RespBulkString("v"), get.execute(new String[]{"GET", "k"}));
    }

    @Test
    void setRejectsWrongArity() {
        assertInstanceOf(RespErr.class, set.execute(new String[]{"SET", "k"}));
    }

    @Test
    void getMissingKeyReturnsNullBulk() {
        assertNull(get.execute(new String[]{"GET", "missing"}));
    }

    @Test
    void getOnWrongTypeReturnsWrongTypeError() {
        store.put("list", new ListValue(new ArrayDeque<>(List.of("a"))));

        assertInstanceOf(RespErr.class, get.execute(new String[]{"GET", "list"}));
    }

    @Test
    void msetStoresSeveralPairs() {
        assertEquals(new RespSimpleString("OK"),
                mset.execute(new String[]{"MSET", "a", "1", "b", "2"}));

        assertEquals(new RespBulkString("1"), get.execute(new String[]{"GET", "a"}));
        assertEquals(new RespBulkString("2"), get.execute(new String[]{"GET", "b"}));
    }

    @Test
    void mgetReturnsNullForMissingKeys() {
        set.execute(new String[]{"SET", "a", "1"});

        RespValue result = mget.execute(new String[]{"MGET", "a", "missing"});

        List<RespValue> values = ((RespArray) result).values();
        assertEquals(new RespBulkString("1"), values.get(0));
        assertNull(values.get(1));
    }

    @Test
    void appendCreatesKeyAndReturnsCharacterCount() {
        RespValue result = append.execute(new String[]{"APPEND", "k", "abc"});

        assertEquals(new RespInteger(3), result);
        assertEquals(new RespBulkString("abc"), get.execute(new String[]{"GET", "k"}));
    }

    @Test
    void appendExtendsExistingKeyAndReturnsByteCount() {
        set.execute(new String[]{"SET", "k", "ab"});

        RespValue result = append.execute(new String[]{"APPEND", "k", "cd"});

        assertEquals(new RespInteger(4), result);
        assertEquals(new RespBulkString("abcd"), get.execute(new String[]{"GET", "k"}));
    }

    @Test
    void strlenCountsBytes() {
        set.execute(new String[]{"SET", "k", "héllo"});

        assertEquals(new RespInteger(6), strlen.execute(new String[]{"STRLEN", "k"}));
    }

    @Test
    void strlenOfMissingKeyIsZero() {
        assertEquals(new RespInteger(0), strlen.execute(new String[]{"STRLEN", "missing"}));
    }

    @Test
    void getdelReturnsAndDeletes() {
        set.execute(new String[]{"SET", "k", "v"});

        assertEquals(new RespBulkString("v"), getdel.execute(new String[]{"GETDEL", "k"}));
        assertNull(get.execute(new String[]{"GET", "k"}));
    }

    @Test
    void getdelMissingKeyReturnsNull() {
        assertNull(getdel.execute(new String[]{"GETDEL", "missing"}));
    }

    @Test
    void incrCreatesAtOneThenIncrements() {
        assertEquals(new RespInteger(1), incr.execute(new String[]{"INCR", "counter"}));
        assertEquals(new RespInteger(2), incr.execute(new String[]{"INCR", "counter"}));
    }

    @Test
    void incrRejectsNonNumericValue() {
        set.execute(new String[]{"SET", "counter", "abc"});

        assertInstanceOf(RespErr.class, incr.execute(new String[]{"INCR", "counter"}));
    }

    @Test
    void decrCreatesAtMinusOneThenDecrements() {
        assertEquals(new RespInteger(-1), decr.execute(new String[]{"DECR", "counter"}));
        assertEquals(new RespInteger(-2), decr.execute(new String[]{"DECR", "counter"}));
    }

    @Test
    void incrByFloatCreatesAtZeroAndAdds() {
        RespValue result = incrByFloat.execute(new String[]{"INCRBYFLOAT", "n", "1.5"});

        assertEquals(new RespBulkString("1.5"), result);
    }

    @Test
    void incrByFloatAccumulates() {
        incrByFloat.execute(new String[]{"INCRBYFLOAT", "n", "1.5"});

        RespValue result = incrByFloat.execute(new String[]{"INCRBYFLOAT", "n", "0.5"});

        assertEquals(new RespBulkString("2"), result);
    }

    @Test
    void incrByFloatRejectsNonFloatDelta() {
        assertInstanceOf(RespErr.class,
                incrByFloat.execute(new String[]{"INCRBYFLOAT", "n", "abc"}));
    }

    @Test
    void setexStoresValueWithTtl() {
        assertEquals(new RespSimpleString("OK"),
                setex.execute(new String[]{"SETEX", "k", "100", "v"}));

        assertEquals(new RespBulkString("v"), get.execute(new String[]{"GET", "k"}));

        RespValue ttlResult = ttl.execute(new String[]{"TTL", "k"});
        assertTrue(((RespInteger) ttlResult).value() > 0);
    }

    @Test
    void setexRejectsNegativeTtl() {
        assertInstanceOf(RespErr.class,
                setex.execute(new String[]{"SETEX", "k", "-1", "v"}));
    }

    @Test
    void wrongTypeIsReportedForStringCommands() {
        store.put("list", new ListValue(new ArrayDeque<>(List.of("a"))));

        assertInstanceOf(RespErr.class, strlen.execute(new String[]{"STRLEN", "list"}));
        assertInstanceOf(RespErr.class, append.execute(new String[]{"APPEND", "list", "x"}));
    }
}
