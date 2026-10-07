package com.capis.Commands;

import com.capis.DataTpes.RespValues.RespErr;
import com.capis.DataTpes.RespValues.RespInteger;
import com.capis.DataTpes.RespValues.RespValue;
import com.capis.entities.KeyValueStore;

public class TTLCommand implements Command {
    private final String name = "TTL";
    private KeyValueStore cache;

    public TTLCommand(KeyValueStore cache) {
        this.cache = cache;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public RespValue execute(String[] args) {
        if(args == null || args.length != 2){
            return new RespErr("ERR", "wrong number of arguments for '" + name + "' command");
        }

        Long ttl = cache.ttlMillis(args[1]);

        if(ttl == null) {
            return new RespInteger(-2);
        }

        if(ttl < 0) {
            return new RespInteger(-1);
        }
        
        return new RespInteger((int) (ttl / 1000));
    }
}
