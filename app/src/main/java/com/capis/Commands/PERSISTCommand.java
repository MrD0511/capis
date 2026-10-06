package com.capis.Commands;

import com.capis.DataTpes.RespValues.RespErr;
import com.capis.DataTpes.RespValues.RespInteger;
import com.capis.DataTpes.RespValues.RespValue;
import com.capis.entities.KeyValueStore;

public class PERSISTCommand implements Command {
    private final String name = "PERSIST";
    private KeyValueStore cache;

    public PERSISTCommand(KeyValueStore cache) {
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

        boolean result = cache.persist(args[1]);
        
        return new RespInteger(result ? 1 : 0);
    }
}