package com.capis.Commands;

import java.util.HashMap;
import java.util.Map;

import com.capis.DataTpes.Core.HashValue;
import com.capis.DataTpes.Core.Value;
import com.capis.DataTpes.RespValues.RespErr;
import com.capis.DataTpes.RespValues.RespInteger;
import com.capis.DataTpes.RespValues.RespValue;
import com.capis.entities.KeyValueStore;

public class HSETCommand implements Command {
    private final String name = "HSET";
    private KeyValueStore cache;

    public HSETCommand(KeyValueStore cache) {
        this.cache = cache;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public RespValue execute(String[] args) {
        if(args == null || args.length % 2 != 0) {
            return new RespErr("ERR", "wrong number of arguments for '" + name + "' command");
        }

        Value<?> existing = cache.get(args[1]);

        HashValue hash;

        if(existing == null) {
            hash = new HashValue(new HashMap<>());
            cache.put(args[1], hash);
        } else if(existing instanceof HashValue) {
            hash = (HashValue) existing;
        } else {
            return new RespErr("ERR", "WRONGTYPE Operation against a key holding the wrong kind of value");
        }

        Map<String, String> fields = hash.getValue();
        int added = 0;

        for(int i = 2; i + 1 < args.length; i += 2) {
            if(!fields.containsKey(args[i])){
                added++;
            }
            fields.put(args[i], args[i + 1]);
        }

        return new RespInteger(added);
    }
}
