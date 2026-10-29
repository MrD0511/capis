package com.capis.Commands;

import java.util.HashSet;
import java.util.Set;

import com.capis.DataTypes.Core.SetValue;
import com.capis.DataTypes.Core.Value;
import com.capis.DataTypes.RespValues.RespErr;
import com.capis.DataTypes.RespValues.RespInteger;
import com.capis.DataTypes.RespValues.RespValue;
import com.capis.entities.KeyValueStore;

public class SADDCommand implements Command {
    private final KeyValueStore cache;
    private final String name = "SADD";

    public SADDCommand(KeyValueStore cache) {
        this.cache = cache;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public RespValue execute(String[] args){
        if(args == null || args.length < 3){
            return new RespErr("ERR", "wrong number of arguments for 'SADD' command");
        }

        String key = args[1];

        Value<?> existing = cache.get(key);

        if (existing != null && !(existing instanceof SetValue)) {
            return new RespErr(
                "WRONGTYPE",
                "Operation against a key holding the wrong kind of value"
            );
        }

        SetValue setValue;

        if(existing == null){
            setValue = new SetValue(new HashSet<>());
            cache.put(key, setValue);
        } else {
            setValue = (SetValue) existing;
        }

        Set<String> set = setValue.getValue();

        int addedCount = 0;
        for (int i = 2; i < args.length; i++) {
            if (set.add(args[i])) {
                addedCount++;
            }
        }

        return new RespInteger(addedCount);
    }
}
