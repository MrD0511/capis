package com.capis.Commands;

import java.util.Arrays;

import com.capis.DataTypes.Core.StringValue;
import com.capis.DataTypes.RespValues.RespArray;
import com.capis.DataTypes.RespValues.RespBulkString;
import com.capis.DataTypes.RespValues.RespErr;
import com.capis.DataTypes.RespValues.RespSimpleString;
import com.capis.DataTypes.RespValues.RespValue;
import com.capis.entities.KeyValueStore;

public class SetCommand implements Command {
    private final String name = "SET";
    private final KeyValueStore keyValueStore;
    private final boolean changesState = true;

    public SetCommand(KeyValueStore keyValueStore) {
        this.keyValueStore = keyValueStore;
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
        if (args.length != 3) {
            return new RespErr("ERR", "SET command requires exactly two arguments.");
        }
        String key = args[1];
        String value = args[2];
        keyValueStore.put(key, new StringValue(value));
        return new RespSimpleString("OK");
    }

    @Override
    public RespValue persistentCommand(String[] args) {
        if (args.length != 3) {
            return new RespErr("ERR", "SET command requires exactly two arguments.");
        }

        return new RespArray(
            Arrays.asList(
                new RespBulkString(name),
                new RespBulkString(args[1]),
                new RespBulkString(args[2])
            )
        );
    }
}
