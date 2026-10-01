package com.capis.Commands;

import java.util.Set;

import com.capis.DataTpes.Core.SetValue;
import com.capis.DataTpes.Core.Value;
import com.capis.DataTpes.RespValues.RespErr;
import com.capis.DataTpes.RespValues.RespInteger;
import com.capis.DataTpes.RespValues.RespValue;
import com.capis.entities.KeyValueStore;

public class SREMCommand implements Command {
    KeyValueStore cache;
    String name = "SREM";

    public SREMCommand(KeyValueStore cache) {
        this.cache = cache;
    }

    public String getName() {
        return name;
    }

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

}
