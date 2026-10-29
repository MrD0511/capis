package com.capis.Commands;

import java.util.ArrayList;
import java.util.List;

import com.capis.DataTypes.RespValues.RespArray;
import com.capis.DataTypes.RespValues.RespBulkString;
import com.capis.DataTypes.RespValues.RespErr;
import com.capis.DataTypes.RespValues.RespInteger;
import com.capis.DataTypes.RespValues.RespValue;
import com.capis.entities.KeyValueStore;

public class DELCommand implements Command {
    private final String name = "DEL";
    private final KeyValueStore cache;
    private final boolean changesState = true;

    public DELCommand(KeyValueStore cache) {
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
        // Implementation for DEL command
        // For each key in args, delete the key from the cache
        if (args == null || args.length < 2) {
            // Handle error: wrong number of arguments
            return new RespErr("ERR", "wrong number of arguments for 'DEL' command");
        }

        int count = 0;
        for (int i = 1; i < args.length; i++) {
            String key = args[i];
            if (cache.remove(key) != null) {
                count++;
            }
        }

        return new RespInteger(count);
    }

    @Override 
    public RespValue persistentCommand(String[] args) {
        if (args == null || args.length < 2) {
            return new RespErr("ERR", "wrong number of arguments for 'DEL' command");
        }

        List<RespValue> keys = new ArrayList<>();

        keys.add(new RespBulkString("DEL"));

        for (int i = 1; i < args.length; i++) {
            keys.add(new RespBulkString(args[i]));
        }   

        return new RespArray(keys);    
    }
}
