package com.capis.Commands;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

import com.capis.DataTypes.Core.ListValue;
import com.capis.DataTypes.Core.Value;
import com.capis.DataTypes.RespValues.RespArray;
import com.capis.DataTypes.RespValues.RespBulkString;
import com.capis.DataTypes.RespValues.RespErr;
import com.capis.DataTypes.RespValues.RespInteger;
import com.capis.DataTypes.RespValues.RespValue;
import com.capis.entities.KeyValueStore;

public class RPushCommand implements Command {

    private final String name = "RPUSH";
    private final KeyValueStore cache;
    private final boolean changesState = true;

    public RPushCommand(KeyValueStore cache) {
        this.cache = cache;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override 
    public boolean changesState() {
        return changesState;
    }

    @Override
    public RespValue execute(String[] args) {

        if (args == null || args.length < 3) {
            return new RespErr(
                "ERR",
                "wrong number of arguments for 'rpush' command"
            );
        }

        String key = args[1];

        Value<?> existingValue = cache.get(key);

        ListValue listValue;

        if (existingValue == null) {
            listValue = new ListValue(new ArrayDeque<>());
            cache.put(key, listValue);
        } else if (existingValue instanceof ListValue listValue1 ) {
            listValue = listValue1;
        } else {
            return new RespErr(
                "WRONGTYPE",
                "Operation against a key holding the wrong kind of value"
            );
        }

        Deque<String> list = listValue.getValue();

        for (int i = 2; i < args.length; i++) {
            list.add(args[i]);
        }

        return new RespInteger(list.size());
    }

    @Override 
    public RespValue persistentCommand(String[] args) {
        if (args == null || args.length < 3) {
            return new RespErr(
                "ERR",
                "wrong number of arguments for 'rpush' command"
            );
        }

        List<RespValue> respValues = new ArrayList<>();

        respValues.add(new RespBulkString("RPUSH"));

        for (int i = 1; i < args.length; i++) {
            respValues.add(new RespBulkString(args[i]));
        }

        return new RespArray(respValues);
    }
}