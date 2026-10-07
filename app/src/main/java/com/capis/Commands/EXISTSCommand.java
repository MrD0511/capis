package com.capis.Commands;

import com.capis.DataTpes.RespValues.RespErr;
import com.capis.DataTpes.RespValues.RespInteger;
import com.capis.DataTpes.RespValues.RespValue;
import com.capis.entities.KeyValueStore;

public class EXISTSCommand implements Command {
    private final String name = "EXISTS";
    private KeyValueStore cache;

    public EXISTSCommand(KeyValueStore cache) {
        this.cache = cache;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public RespValue execute(String[] args) {
        if(args == null || args.length < 2) {
            return new RespErr("ERR", "wrong number of arguments for 'exists' command");
        }

        int count = 0;
        for (int i = 1; i < args.length; i++) {
            if(cache.get(args[i]) != null) {
                count++;
            }
        }

        return new RespInteger(count);
    }
}
