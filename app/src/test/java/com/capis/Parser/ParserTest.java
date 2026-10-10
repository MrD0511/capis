
package com.capis.Parser;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ParserTest {

    private final Parser parser = new Parser();

    @Test 
    void parsesPING() {
        String input = "+PING\r\n";

        String[] result = parser.decode(toBuffer(input));

        assertArrayEquals(new String[]{"PING"}, result);
    }

    @Test
    void parsesSimpleString() {
        String input = "+OK\r\n";

        String[] result = parser.decode(toBuffer(input));

        assertArrayEquals(new String[]{"OK"}, result);
    }

    @Test
    void parsesInteger() {
        String input = ":1000\r\n";

        String[] result = parser.decode(toBuffer(input));

        assertArrayEquals(new String[]{"1000"}, result);
    }

    @Test
    void parsesBulkString() {
        String input = "$6\r\nfoobar\r\n";

        String[] result = parser.decode(toBuffer(input));

        assertArrayEquals(new String[]{"foobar"}, result);
    }

    @Test
    void parsesArray() {
        String input =
                "*2\r\n" +
                "$3\r\nfoo\r\n" +
                "$3\r\nbar\r\n";

        String[] result = parser.decode(toBuffer(input));

        assertArrayEquals(new String[]{"foo", "bar"}, result);
    }

    @Test 
    void parsesNestedArray() {
        String input =
                "*2\r\n" +
                "*2\r\n" +
                "$3\r\nfoo\r\n" +
                "$3\r\nbar\r\n" +
                "*2\r\n" +
                "$3\r\nbaz\r\n" +
                "$3\r\nqux\r\n";

        String[] result = parser.decode(toBuffer(input));

        assertArrayEquals(new String[]{"foo", "bar", "baz", "qux"}, result);
    }

    
    @Test
    void partialRead() {
        String firstChunk = "*2\r\n$3\r\nfoo\r\n$3\r\n";
        String secondChunk = "bar\r\n";

        ByteBuffer firstBuffer = toBuffer(firstChunk);
        String[] firstResult = parser.decode(firstBuffer);

        assertNull(firstResult);

        // In a real server, preserve the unconsumed bytes
        // and append the bytes from the next socket read.
        ByteBuffer combined = toBuffer(firstChunk + secondChunk);

        String[] result = parser.decode(combined);

        assertArrayEquals(
            new String[]{"foo", "bar"},
            result
        );
    }

    @Test
    void parsesPipelinedCommands() {
        String input =
                "*3\r\n$3\r\nSET\r\n$8\r\ngreeting\r\n$5\r\nhello\r\n" +
                "*2\r\n$3\r\nGET\r\n$8\r\ngreeting\r\n";

        ByteBuffer buffer = toBuffer(input);

        String[] first = parser.decode(buffer);

        assertArrayEquals(
                new String[]{"SET", "greeting", "hello"},
                first
        );

        String[] second = parser.decode(buffer);

        assertArrayEquals(
                new String[]{"GET", "greeting"},
                second
        );
    }

    private ByteBuffer toBuffer(String input) {
        return ByteBuffer.wrap(
                input.getBytes(StandardCharsets.UTF_8)
        );
    }
}