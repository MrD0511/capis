package com.capis.Commands;

import com.capis.DataTpes.RespValues.RespErr;
import com.capis.DataTpes.RespValues.RespSimpleString;
import com.capis.DataTpes.RespValues.RespValue;
import com.capis.entities.KeyValueStore;

public class FLUSHALLCommand implements Command {
    private final String name = "FLUSHALL";
    private KeyValueStore cache;

    public FLUSHALLCommand(KeyValueStore cache) {
        this.cache = cache;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public RespValue execute(String[] args) {
        if(args == null || args.length != 1){
            return new RespErr("ERR", "wrong number of arguments for '" + name + "' command");
        }

        cache.clear();
        
        return new RespSimpleString("OK");
    }
}
