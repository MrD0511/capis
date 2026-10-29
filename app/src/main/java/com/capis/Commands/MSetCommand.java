package com.capis.Commands;

import java.util.ArrayList;
import java.util.List;

import com.capis.DataTypes.Core.StringValue;
import com.capis.DataTypes.RespValues.RespArray;
import com.capis.DataTypes.RespValues.RespBulkString;
import com.capis.DataTypes.RespValues.RespErr;
import com.capis.DataTypes.RespValues.RespSimpleString;
import com.capis.DataTypes.RespValues.RespValue;
import com.capis.entities.KeyValueStore;

public class MSetCommand implements Command {
    private final String name = "MSET";
    private final KeyValueStore cache;
    private final boolean changesState = true;

    public MSetCommand(KeyValueStore cache) {
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
        // Implementation for MSET command
        // For each key-value pair in args, set the key to the value
        if (args == null || args.length < 3 || args.length % 2 == 0) {
            return new RespErr(
                "ERR",
                "wrong number of arguments for 'mset' command."
            );
        }

        for (int i = 1; i+1 < args.length; i += 2) {
            String key = args[i];
            String value = args[i + 1];

            if(key == null || value == null || key.isEmpty() || value.isEmpty()) {
                return new RespErr("ERR", "wrong number of arguments for 'mset' command.");
            }

            cache.put(key, new StringValue(value));
        }
        
        return new RespSimpleString("OK"); // Placeholder return value
    }

    @Override
    public RespValue persistentCommand(String[] args) {
        if(args == null || args.length < 3 || args.length % 2 == 0) {
            return new RespErr(
                "ERR",
                "wrong number of arguments for 'mset' command."
            );
        }

        List<RespValue> respValues = new ArrayList<>();

        respValues.add(new RespBulkString("MSET"));

        for (int i = 1; i < args.length; i++) {
            respValues.add(new RespBulkString(args[i]));
        }
        
        return new RespArray(respValues);
    }
}
