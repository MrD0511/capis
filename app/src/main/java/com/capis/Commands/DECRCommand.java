package com.capis.Commands;

import java.util.Arrays;

import com.capis.DataTypes.Core.Value;
import com.capis.DataTypes.RespValues.RespArray;
import com.capis.DataTypes.RespValues.RespBulkString;
import com.capis.DataTypes.RespValues.RespErr;
import com.capis.DataTypes.RespValues.RespInteger;
import com.capis.DataTypes.RespValues.RespValue;
import com.capis.entities.KeyValueStore;

public class DECRCommand implements Command{
    private final KeyValueStore cache;

    private final String name = "DECR";

    private final boolean changesState = true;

    public DECRCommand(KeyValueStore cache) {
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
        if (args == null || args.length != 2) {
            return new RespErr(
                "ERR",
                "wrong number of arguments for 'DECR' command"
            );
        }

        String key = args[1];

        try {
            Value<?> value = cache.decrement(key);

            return new RespInteger(
                Integer.parseInt((String) value.getValue())
            );

        } catch (IllegalArgumentException e) {
            return new RespErr(
                "ERR",
                "value is not an integer or out of range"
            );
        }
    }

    @Override 
    public RespValue persistentCommand(String[] args) {
        if (args == null || args.length != 2) {
            return new RespErr(
                "ERR",
                "wrong number of arguments for 'DECR' command"
            );
        }

        String key = args[1];

        return new RespArray(
            Arrays.asList(
                new RespBulkString("DECR"),
                new RespBulkString(key)
            )
        );
    }
}
