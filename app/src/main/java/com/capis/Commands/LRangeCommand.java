package com.capis.Commands;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Queue;

import com.capis.DataTpes.Core.ListValue;
import com.capis.DataTpes.Core.Value;
import com.capis.DataTpes.RespValues.RespArray;
import com.capis.DataTpes.RespValues.RespBulkString;
import com.capis.DataTpes.RespValues.RespErr;
import com.capis.DataTpes.RespValues.RespValue;
import com.capis.entities.KeyValueStore;

public class LRangeCommand implements Command {

    private final String name = "LRANGE";
    private final KeyValueStore cache;

    public LRangeCommand(KeyValueStore cache) {
        this.cache = cache;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public RespValue execute(String[] args) {

        if (args == null || args.length != 4) {
            return new RespErr(
                "ERR",
                "wrong number of arguments for 'lrange' command"
            );
        }

        int start;
        int stop;

        try {
            start = Integer.parseInt(args[2]);
            stop = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            return new RespErr(
                "ERR",
                "value is not an integer or out of range"
            );
        }

        Value<?> value = cache.get(args[1]);

        if (value == null) {
            return new RespArray(List.of());
        }

        if (!(value instanceof ListValue )) {
            return new RespErr(
                "WRONGTYPE",
                "Operation against a key holding the wrong kind of value"
            );
        }

        ListValue listValue = (ListValue) value;

        Deque<String> list = listValue.getValue();
        int size = list.size();

        if (start < 0) {
            start = size + start;
        }

        if (stop < 0) {
            stop = size + stop;
        }

        start = Math.max(start, 0);
        stop = Math.min(stop, size - 1);
        
        List<RespValue> result = new ArrayList<>();

        if (start <= stop && start < size) {
            int index = 0;

            for (String element : list) {
                if (index > stop) {
                    break;
                }

                if (index >= start) {
                    result.add(new RespBulkString(element));
                }

                index++;
            }
        }

        return new RespArray(result);
    }
}