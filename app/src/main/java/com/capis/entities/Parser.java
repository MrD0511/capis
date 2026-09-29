package com.capis.entities;

import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.ArrayList;

public class Parser {

    // Main entry point for clients
    public String[] parse(InputStream input) {
        try {
            List<String> result = new ArrayList<>();
            parseElement(input, result);
            return result.toArray(new String[0]);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * The core recursive engine. Evaluates the type prefix 
     * and streams findings into the target accumulator list.
     */
    private void parseElement(InputStream input, List<String> accumulator) throws Exception {
        int typeByte = input.read();
        if (typeByte == -1) {
            return;
        }

        char type = (char) typeByte;

        switch (type) {
            case '+': // Simple String
                accumulator.add(readLine(input));
                break;
                
            case '-': // Error String
                accumulator.add(readLine(input));
                break;
                
            case ':': // Integer
                String intLine = readLine(input);
                if (intLine == null || intLine.isEmpty()) {
                    throw new Exception("Malformed RESP: Empty integer payload.");
                }
                accumulator.add(intLine);
                break;
                
            case '$': // Bulk String
                String bulkLengthLine = readLine(input);
                if (bulkLengthLine == null || bulkLengthLine.isEmpty()) {
                    throw new Exception("Malformed RESP: Empty bulk length.");
                }

                long bulkLength = Long.parseLong(bulkLengthLine);
                if (bulkLength == -1) {
                    accumulator.add(null); // Explicit Null Bulk String
                    break;
                }

                accumulator.add(readBulkString(input, bulkLength));
                break;

            case '*': // Array (Recursion magic happens here!)
                String arrayLengthLine = readLine(input);
                long arrayLength = Long.parseLong(arrayLengthLine);
                if (arrayLength == -1) {
                    accumulator.add(null); // Null Array support
                    break;
                }

                // Loop and recursively call parseElement for every single element
                for (int i = 0; i < arrayLength; i++) {
                    parseElement(input, accumulator);
                }
                break;

            default:
                throw new Exception("Unknown RESP type byte: " + type);
        }
    }

    private String readLine(InputStream input) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int b;

        while ((b = input.read()) != -1) {
            if (b == '\r') {
                int next = input.read();
                if (next == '\n') {
                    break;
                }
                buffer.write(b);
                if (next != -1) {
                    buffer.write(next);
                }
            } else {
                buffer.write(b);
            }
        }
        return buffer.toString("UTF-8");
    }

    private String readBulkString(InputStream input, long length) throws Exception {
        byte[] buffer = new byte[(int) length];
        int bytesRead = 0;

        while (bytesRead < length) {
            int read = input.read(buffer, bytesRead, (int) length - bytesRead);
            if (read == -1) {
                throw new Exception("Unexpected end of stream while reading bulk string.");
            }
            bytesRead += read;
        }

        int r = input.read();
        int n = input.read();
        if (r != '\r' || n != '\n') {
            throw new Exception("Malformed RESP: Bulk string data not followed by CRLF");
        }

        return new String(buffer, "UTF-8");
    }
}
