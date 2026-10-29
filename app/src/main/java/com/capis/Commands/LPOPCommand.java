package com.capis.Commands;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import com.capis.DataTypes.Core.ListValue;
import com.capis.DataTypes.Core.Value;
import com.capis.DataTypes.RespValues.RespArray;
import com.capis.DataTypes.RespValues.RespBulkString;
import com.capis.DataTypes.RespValues.RespErr;
import com.capis.DataTypes.RespValues.RespValue;
import com.capis.entities.KeyValueStore;

public class LPOPCommand implements Command {

    private final String name = "LPOP";
    private final KeyValueStore cache;

    public LPOPCommand(KeyValueStore cache) {
        this.cache = cache;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public RespValue execute(String[] args) {

        if (args == null || (args.length != 2 && args.length != 3)) {
            return new RespErr(
                "ERR",
                "wrong number of arguments for 'LPOP' command"
            );
        }

        int count = 1;

        if (args.length == 3) {
            try {
                count = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                return new RespErr(
                    "ERR",
                    "value is not an integer or out of range"
                );
            }
        }

        if (count < 0) {
            return new RespErr(
                "ERR",
                "value is not an integer or out of range"
            );
        }

        Value<?> existing = cache.get(args[1]);

        if (existing == null) {
            return count == 1
                ? null
                : new RespArray(List.of());
        }

        if (!(existing instanceof ListValue)) {
            return new RespErr(
                "WRONGTYPE",
                "Operation against a key holding the wrong kind of value"
            );
        }

        ListValue listValue = (ListValue) existing;

        if (count == 0 || listValue.getValue().isEmpty()) {
            return count == 1
                ? null
                : new RespArray(List.of());
        }

        if (count == 1) {
            String removed = listValue.getValue().poll();

            if (listValue.getValue().isEmpty()) {
                cache.remove(args[1]);
            }

            return new RespBulkString(removed);
        }

        List<RespValue> removedElements = new ArrayList<>();
        Deque<String> deque = listValue.getValue();

        for (int i = 0; i < count && !deque.isEmpty(); i++) {
            String removed = deque.poll();
            removedElements.add(new RespBulkString(removed));
        }

        if (listValue.getValue().isEmpty()) {
            cache.remove(args[1]);
        }

        return new RespArray(removedElements);
    }
}