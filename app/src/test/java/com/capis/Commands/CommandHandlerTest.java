package com.capis.Commands;

import java.util.List;

import com.capis.DataTpes.Core.StringValue;
import com.capis.DataTpes.RespValues.RespArray;
import com.capis.DataTpes.RespValues.RespBulkString;
import com.capis.DataTpes.RespValues.RespErr;
import com.capis.DataTpes.RespValues.RespSimpleString;
import com.capis.DataTpes.RespValues.RespValue;
import com.capis.Network.ClientState;
import com.capis.entities.KeyValueStore;
import com.capis.entities.LRUCache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandHandlerTest {

    private KeyValueStore store;
    private CommandHandler handler;
    private ClientState clientState;

    @BeforeEach
    void setUp() {
        store = new KeyValueStore(new LRUCache<>(100, 256));

        CommandRegistry registry = new CommandRegistry();
        registry.register(new SetCommand(store));
        registry.register(new GetCommand(store));
        registry.register(new PingCommand());

        handler = new CommandHandler(registry);
        clientState = new ClientState();
    }

    @Test
    void dispatchesByCaseInsensitiveName() {
        RespValue lower = handler.execute(new String[]{"set", "k", "v"}, clientState);
        RespValue upper = handler.execute(new String[]{"SET", "k2", "v2"}, clientState);

        assertEquals(new RespSimpleString("OK"), lower);
        assertEquals(new RespSimpleString("OK"), upper);
        assertEquals(new RespBulkString("v"), handler.execute(new String[]{"GET", "k"}, clientState));
    }

    @Test
    void unknownCommandRepliesWithError() {
        RespValue result = handler.execute(new String[]{"NOPE"}, clientState);

        assertInstanceOf(RespErr.class, result);
        assertEquals("Unknown command 'NOPE'", ((RespErr) result).message());
    }

    @Test
    void emptyCommandRepliesWithError() {
        RespValue result = handler.execute(new String[]{}, clientState);

        assertInstanceOf(RespErr.class, result);
    }

    @Test
    void multiExecRunsQueuedCommandsInOrder() {
        assertEquals(new RespSimpleString("OK"),
                handler.execute(new String[]{"MULTI"}, clientState));
        assertTrue(handler.execute(new String[]{"SET", "k", "v"}, clientState)
                instanceof RespSimpleString simple && simple.value().equals("QUEUED"));
        assertTrue(handler.execute(new String[]{"SET", "k2", "v2"}, clientState)
                instanceof RespSimpleString simple && simple.value().equals("QUEUED"));

        RespValue result = handler.execute(new String[]{"EXEC"}, clientState);

        assertInstanceOf(RespArray.class, result);
        List<RespValue> values = ((RespArray) result).values();
        assertEquals(2, values.size());
        assertEquals(new RespSimpleString("OK"), values.get(0));
        assertEquals(new RespSimpleString("OK"), values.get(1));
        assertEquals("v", ((StringValue) store.get("k")).getValue());
        assertEquals("v2", ((StringValue) store.get("k2")).getValue());
    }

    @Test
    void commandsRunInsideTransactionAreNotVisibleUntilExec() {
        handler.execute(new String[]{"MULTI"}, clientState);
        handler.execute(new String[]{"SET", "k", "v"}, clientState);

        assertNull(store.get("k"));

        handler.execute(new String[]{"EXEC"}, clientState);

        assertEquals("v", ((StringValue) store.get("k")).getValue());
    }

    @Test
    void nestedMultiIsRejected() {
        handler.execute(new String[]{"MULTI"}, clientState);

        RespValue result = handler.execute(new String[]{"MULTI"}, clientState);

        assertInstanceOf(RespErr.class, result);
        assertEquals("MULTI calls can not be nested", ((RespErr) result).message());
    }

    @Test
    void execWithoutMultiIsRejected() {
        RespValue result = handler.execute(new String[]{"EXEC"}, clientState);

        assertInstanceOf(RespErr.class, result);
        assertEquals("EXEC without MULTI", ((RespErr) result).message());
    }

    @Test
    void discardWithoutMultiIsRejected() {
        RespValue result = handler.execute(new String[]{"DISCARD"}, clientState);

        assertInstanceOf(RespErr.class, result);
        assertEquals("DISCARD without MULTI", ((RespErr) result).message());
    }

    @Test
    void discardDropsTheQueue() {
        handler.execute(new String[]{"MULTI"}, clientState);
        handler.execute(new String[]{"SET", "k", "v"}, clientState);

        assertEquals(new RespSimpleString("OK"),
                handler.execute(new String[]{"DISCARD"}, clientState));

        assertNull(store.get("k"));
        assertEquals("EXEC without MULTI",
                ((RespErr) handler.execute(new String[]{"EXEC"}, clientState)).message());
    }

    @Test
    void execReturnsErrorsFromUnknownQueuedCommands() {
        handler.execute(new String[]{"MULTI"}, clientState);
        // Queue-time validation is missing, so an unknown command is still queued.
        handler.execute(new String[]{"NOPE"}, clientState);

        RespValue result = handler.execute(new String[]{"EXEC"}, clientState);

        assertInstanceOf(RespArray.class, result);
        List<RespValue> values = ((RespArray) result).values();
        assertEquals(1, values.size());
        assertInstanceOf(RespErr.class, values.get(0));
    }
}
