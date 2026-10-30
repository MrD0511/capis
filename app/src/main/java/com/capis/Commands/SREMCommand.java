package com.capis.Commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.capis.DataTypes.Core.SetValue;
import com.capis.DataTypes.Core.Value;
import com.capis.DataTypes.RespValues.RespArray;
import com.capis.DataTypes.RespValues.RespBulkString;
import com.capis.DataTypes.RespValues.RespErr;
import com.capis.DataTypes.RespValues.RespInteger;
import com.capis.DataTypes.RespValues.RespValue;
import com.capis.entities.KeyValueStore;

public class SREMCommand implements Command {
    private final KeyValueStore cache;
    private final String name = "SREM";
    private final boolean changesState = true;

    public SREMCommand(KeyValueStore cache) {
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
            return new RespErr("ERR", "wrong number of arguments for 'SREM' command");
        }

        String key = args[1];

        Value<?> existing = cache.get(key);

        if (existing == null) {
            return new RespInteger(0);
        }

        if (!(existing instanceof SetValue)) {
            return new RespErr(
                "WRONGTYPE",
                "Operation against a key holding the wrong kind of value"
            );
        }

        SetValue setValue = (SetValue) existing;
        Set<String> set = setValue.getValue();

        int removedCount = 0;

        for (int i = 2; i < args.length; i++) {
            if (set.remove(args[i])) {
                removedCount++;
            }
        }

        if(set.isEmpty()) {
            cache.remove(key);
        }

        return new RespInteger(removedCount);
    }

    @Override 
    public RespValue persistentCommand(String[] args) {
        if (args == null || args.length < 3) {
            return new RespErr("ERR", "wrong number of arguments for 'SREM' command");
        }

        List<RespValue> respValues = new ArrayList<>();
    
        respValues.add(new RespBulkString("SREM"));

        for(int i = 1; i < args.length; i++) {
            respValues.add(new RespBulkString(args[i]));
        }

        return new RespArray(respValues);
    }

}
