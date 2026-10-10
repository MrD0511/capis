package com.capis.Parser;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.capis.DataTpes.RespValues.RespArray;
import com.capis.DataTpes.RespValues.RespBulkString;
import com.capis.DataTpes.RespValues.RespErr;
import com.capis.DataTpes.RespValues.RespInteger;
import com.capis.DataTpes.RespValues.RespSimpleString;
import com.capis.DataTpes.RespValues.RespValue;

public class Parser {

    private boolean isRespValue(byte byt){
        char c = (char) byt;
        return c == '+' || c == '-' || c == ':' || c == '$' || c == '*';
    }

    // Main entry point for clients
    public String[] decode(ByteBuffer buffer) {
        buffer.mark();



        try {
            byte first = buffer.get(buffer.position());
            if(!isRespValue(first)){
                String line = readLine(buffer);
                if(line == null) return null;

                if(line.isEmpty()){     
                    return null;
                }

                return line.strip().split("\\s+");
            }

            List<String> result = new ArrayList<>();

            boolean success = parseElement(buffer, result);

            if(!success){
                buffer.reset();
                return null;
            }

            return result.toArray(new String[0]);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return null;
    }

    public String encode(RespValue value){
        if(value == null){
            return "$-1\r\n";
        }

        if(value instanceof RespSimpleString simple){
            return "+" + simple.value() + "\r\n";
        }

        if(value instanceof RespInteger integer){
            return ":" + integer.value() + "\r\n";
        }
        
        if(value instanceof RespBulkString bulk){
            return serializeBulkString(bulk.value());
        }

        if(value instanceof RespErr err){
            return "-" + err.type() + " " + err.message() + "\r\n";
        }

        if(value instanceof RespArray array){
            return serializeArray(array);
        }
        
        throw new IllegalArgumentException("Unsupported RespValue type: " + value.getClass().getName());
    }
    
    // Core serialization logic
    private String serializeBulkString(String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);

        return "$" + bytes.length + "\r\n"
                + value
                + "\r\n";
    }

    private String serializeArray(RespArray value) {

        StringBuilder result = new StringBuilder();

        result.append("*")
              .append(value.values().size())
              .append("\r\n");

        for (RespValue element : value.values()) {
            result.append(encode(element));
        }

        return result.toString();
    }
    

    // Core parsing logic
    private boolean parseElement(ByteBuffer buffer, List<String> accumulator) throws Exception {
        if (!buffer.hasRemaining()) {
            return false;
        }

        buffer.mark();

        int typeByte = buffer.get() & 0xFF;
        char type = (char) typeByte;

        switch (type) {
            case '+', '-':
                String line = readLine(buffer);

                if(line == null){
                    return false;
                }
                accumulator.add(line);
                return true;

            case ':': // Integer
                String intLine = readLine(buffer);

                if(intLine == null){
                    return false;
                }

                if (intLine.isEmpty()) {
                    throw new Exception("Malformed RESP: Empty integer payload.");
                }
                
                accumulator.add(intLine);
                
                return true;

            case '$': // Bulk String
                String bulkLengthLine = readLine(buffer);
                if (bulkLengthLine == null) return false;
                if (bulkLengthLine.isEmpty()) {
                    throw new Exception("Malformed RESP: Empty bulk length.");
                }

                long bulkLength = Long.parseLong(bulkLengthLine);
                if (bulkLength == -1) {
                    accumulator.add(null); 
                    return true;
                }

                String bulkData = readBulkString(buffer, bulkLength);
                if (bulkData == null) return false;
                
                accumulator.add(bulkData);
                return true;

            case '*': // Array (Recursion magic happens here!)
                String arrayLengthLine = readLine(buffer);
                if (arrayLengthLine == null) return false;
                if (arrayLengthLine.isEmpty()) {
                    throw new Exception("Malformed RESP: Empty array length.");
                }

                long arrayLength = Long.parseLong(arrayLengthLine);
                if (arrayLength == -1) {
                    accumulator.add(null);
                    return true;
                }

                for (int i = 0; i < arrayLength; i++) {
                    // If any nested child element is incomplete, fail the whole chain
                    if (!parseElement(buffer, accumulator)) {
                        return false;
                    }
                }
                return true;

            default:
                throw new Exception("Unknown RESP type byte: " + type);
        }
    }

    private String readLine(ByteBuffer inputBuffer) throws Exception {
        inputBuffer.mark();

        int lineEndPos = -1;
        int startPos = inputBuffer.position();
        boolean partialCRfound = false;

        while(inputBuffer.hasRemaining()){
            byte b = inputBuffer.get();
            
            if(b == '\r'){
                if(inputBuffer.hasRemaining()){
                    if(inputBuffer.get() == '\n'){
                        lineEndPos = inputBuffer.position() - 2;
                        break;
                    }else{
                        throw new Exception("Malformed RESP: Expected LF after CR");
                    }
                }else{
                    partialCRfound = true;
                    break;
                }
            }
        }

        if(lineEndPos == -1){
            inputBuffer.reset();

            if(inputBuffer.position() == 0 && inputBuffer.limit() == inputBuffer.capacity() - 1 && !partialCRfound){
                throw new Exception("Malformed RESP: Line too long or no CRLF found.");
            }

            return null;
        }

        int currentPos = inputBuffer.position();
        inputBuffer.reset();

        int length = lineEndPos - startPos;
        byte[] lineBytes = new byte[length];
        inputBuffer.get(lineBytes);

        inputBuffer.position(currentPos);

        return new String(lineBytes, StandardCharsets.UTF_8);
    }

    private String readBulkString(ByteBuffer inputBuffer, long length) throws Exception {
        if (length < 0) {
            throw new Exception("Malformed RESP: Negative bulk string length.");
        }

        inputBuffer.mark();

        if(inputBuffer.remaining() < length + 2){
            inputBuffer.reset();
            return null;
        }

        byte[] buffer = new byte[(int) length];
        inputBuffer.get(buffer);

        int r = inputBuffer.get() & 0xFF;
        int n = inputBuffer.get() & 0xFF;
        if (r != '\r' || n != '\n') {
            throw new Exception("Malformed RESP: Bulk string data not followed by CRLF");
        }

        String resultString = new String(buffer, StandardCharsets.UTF_8);

        return resultString;
    }
}
