package com.capis.Commands;

import java.util.List;

import com.capis.DataTpes.Core.ListValue;
import com.capis.DataTpes.RespValues.RespInteger;
import com.capis.DataTpes.RespValues.RespSimpleString;
import com.capis.entities.KeyValueStore;
import com.capis.entities.LRUCache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KeyCommandsTest {

    private KeyValueStore store;
    private SetCommand set;
    private DELCommand del;
    private EXISTSCommand exists;
    private TypeCommand type;
    private DBSIZECommand dbsize;
    private FLUSHALLCommand flushall;
    private EXPIRECommand expire;
    private TTLCommand ttl;
    private PERSISTCommand persist;

    @BeforeEach
    void setUp() {
        store = new KeyValueStore(new LRUCache<>(100, 256));
        set = new SetCommand(store);
        del = new DELCommand(store);
        exists = new EXISTSCommand(store);
        type = new TypeCommand(store);
        dbsize = new DBSIZECommand(store);
        flushall = new FLUSHALLCommand(store);
        expire = new EXPIRECommand(store);
        ttl = new TTLCommand(store);
        persist = new PERSISTCommand(store);

        set.execute(new String[]{"SET", "a", "1"});
        set.execute(new String[]{"SET", "b", "2"});
        set.execute(new String[]{"SET", "c", "3"});
    }

    @Test
    void delCountsRemovedKeys() {
        assertEquals(new RespInteger(2),
                del.execute(new String[]{"DEL", "a", "b", "missing"}));
        assertEquals(new RespInteger(1), dbsize.execute(new String[]{"DBSIZE"}));
    }

    @Test
    void existsCountsPerOccurrence() {
        assertEquals(new RespInteger(3),
                exists.execute(new String[]{"EXISTS", "a", "b", "c"}));
        assertEquals(new RespInteger(2),
                exists.execute(new String[]{"EXISTS", "a", "a"}));
        assertEquals(new RespInteger(0),
                exists.execute(new String[]{"EXISTS", "missing"}));
    }

    @Test
    void typeReportsTheValueKind() {
        assertEquals(new RespSimpleString("string"),
                type.execute(new String[]{"TYPE", "a"}));

        store.put("list", new ListValue(List.of("x")));
        assertEquals(new RespSimpleString("list"),
                type.execute(new String[]{"TYPE", "list"}));

        assertEquals(new RespSimpleString("none"),
                type.execute(new String[]{"TYPE", "missing"}));
    }

    @Test
    void dbsizeCountsLiveKeys() {
        assertEquals(new RespInteger(3), dbsize.execute(new String[]{"DBSIZE"}));
    }

    @Test
    void flushallRemovesEverything() {
        assertEquals(new RespSimpleString("OK"), flushall.execute(new String[]{"FLUSHALL"}));
        assertEquals(new RespInteger(0), dbsize.execute(new String[]{"DBSIZE"}));
    }

    @Test
    void expireReturnsOneForExistingKeyAndZeroOtherwise() {
        assertEquals(new RespInteger(1), expire.execute(new String[]{"EXPIRE", "a", "100"}));
        assertEquals(new RespInteger(0), expire.execute(new String[]{"EXPIRE", "missing", "100"}));
    }

    @Test
    void expireRejectsNegativeSeconds() {
        assertEquals("ERR",
                ((com.capis.DataTpes.RespValues.RespErr) expire.execute(
                        new String[]{"EXPIRE", "a", "-1"})).type());
    }

    @Test
    void ttlReportsMissingAndNoExpiry() {
        assertEquals(new RespInteger(-1), ttl.execute(new String[]{"TTL", "a"}));
        assertEquals(new RespInteger(-2), ttl.execute(new String[]{"TTL", "missing"}));
    }

    @Test
    void ttlReportsRemainingSecondsAfterExpire() {
        expire.execute(new String[]{"EXPIRE", "a", "100"});

        int remaining = ((RespInteger) ttl.execute(new String[]{"TTL", "a"})).value();
        assertEquals(true, remaining > 0 && remaining <= 100);
    }

    @Test
    void persistClearsTheTtl() {
        expire.execute(new String[]{"EXPIRE", "a", "100"});

        assertEquals(new RespInteger(1), persist.execute(new String[]{"PERSIST", "a"}));
        assertEquals(new RespInteger(-1), ttl.execute(new String[]{"TTL", "a"}));
        assertEquals(new RespInteger(0), persist.execute(new String[]{"PERSIST", "a"}));
    }
}
