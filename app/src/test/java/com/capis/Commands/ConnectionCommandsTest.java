package com.capis.Commands;

import com.capis.DataTypes.RespValues.RespArray;
import com.capis.DataTypes.RespValues.RespBulkString;
import com.capis.DataTypes.RespValues.RespErr;
import com.capis.DataTypes.RespValues.RespInteger;
import com.capis.DataTypes.RespValues.RespSimpleString;
import com.capis.DataTypes.RespValues.RespValue;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class ConnectionCommandsTest {

    private final PingCommand ping = new PingCommand();
    private final EchoCommand echo = new EchoCommand();
    private final HELLOCommand hello = new HELLOCommand();

    @Test
    void barePingRepliesPong() {
        assertEquals(new RespSimpleString("PONG"), ping.execute(new String[]{"PING"}));
    }

    @Test
    void pingWithMessageEchoesItAsBulkString() {
        assertEquals(new RespBulkString("hi"), ping.execute(new String[]{"PING", "hi"}));
    }

    @Test
    void pingWithTooManyArgumentsErrors() {
        assertInstanceOf(RespErr.class, ping.execute(new String[]{"PING", "a", "b"}));
    }

    @Test
    void bareEchoRepliesECHO() {
        assertEquals(new RespSimpleString("ECHO"), echo.execute(new String[]{"ECHO"}));
    }

    @Test
    void echoReturnsTheMessage() {
        assertEquals(new RespBulkString("hello"), echo.execute(new String[]{"ECHO", "hello"}));
    }

    @Test
    void helloWithoutVersionReturnsServerInfo() {
        RespValue result = hello.execute(new String[]{"HELLO"});

        assertInstanceOf(RespArray.class, result);

        Map<String, RespValue> fields = toMap(((RespArray) result).values());

        assertEquals(new RespBulkString("capis"), fields.get("server"));
        assertEquals(new RespBulkString("1.0.0"), fields.get("version"));
        assertEquals(new RespInteger(2), fields.get("proto"));
        assertEquals(new RespBulkString("standalone"), fields.get("mode"));
        assertEquals(new RespBulkString("master"), fields.get("role"));
    }

    @Test
    void helloWithProtocolTwoIsAccepted() {
        RespValue result = hello.execute(new String[]{"HELLO", "2"});

        assertInstanceOf(RespArray.class, result);
    }

    @Test
    void helloWithOtherProtocolIsRejected() {
        RespValue result = hello.execute(new String[]{"HELLO", "3"});

        assertInstanceOf(RespErr.class, result);
        assertEquals("NOPROTO", ((RespErr) result).type());
    }

    private Map<String, RespValue> toMap(List<RespValue> flat) {
        Map<String, RespValue> fields = new java.util.HashMap<>();

        for (int i = 0; i + 1 < flat.size(); i += 2) {
            fields.put(((RespBulkString) flat.get(i)).value(), flat.get(i + 1));
        }

        return fields;
    }
}
