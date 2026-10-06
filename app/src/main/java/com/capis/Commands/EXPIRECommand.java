package com.capis.Commands;

import java.util.concurrent.TimeUnit;

import com.capis.DataTpes.RespValues.RespErr;
import com.capis.DataTpes.RespValues.RespInteger;
import com.capis.DataTpes.RespValues.RespValue;
import com.capis.entities.KeyValueStore;

public class EXPIRECommand implements Command {
    private final String name = "EXPIRE";
    private KeyValueStore cache;

    public EXPIRECommand(KeyValueStore cache) {
        this.cache = cache;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override 
    public RespValue execute(String[] args) {
        if(args == null || args.length != 3){
            return null; // Placeholder for error response
        }

        int seconds;

        try {
            seconds = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            return new RespErr("ERR value is not an integer or out of range", name);
        }

        if(seconds < 0) {
            return new RespErr("ERR", "invalid expire time in '" + name + "' command");
        }

        return new RespInteger(
                cache.expire(args[1], TimeUnit.SECONDS.toMillis(seconds)) ? 1 : 0
            );
    }
}
