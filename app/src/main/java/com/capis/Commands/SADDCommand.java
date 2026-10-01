package com.capis.Commands;

import java.util.HashSet;
import java.util.Set;

import com.capis.DataTpes.Core.SetValue;
import com.capis.DataTpes.Core.Value;
import com.capis.DataTpes.RespValues.RespErr;
import com.capis.DataTpes.RespValues.RespInteger;
import com.capis.DataTpes.RespValues.RespValue;
import com.capis.entities.KeyValueStore;

public class SADDCommand implements Command {
    KeyValueStore cache;
    String name = "SADD";

    public SADDCommand(KeyValueStore cache) {
        this.cache = cache;
    }

    public String getName() {
        return name;
    }


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

        for (int i = 2; i < args.length; i++) {
            set.add(args[i]);
        }

        return new RespInteger(set.size());

    }
}
