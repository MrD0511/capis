package com.capis.Commands;

import java.util.concurrent.TimeUnit;

import com.capis.DataTpes.Core.StringValue;
import com.capis.DataTpes.RespValues.RespErr;
import com.capis.DataTpes.RespValues.RespSimpleString;
import com.capis.DataTpes.RespValues.RespValue;
import com.capis.entities.KeyValueStore;

public class SETEXCommand implements Command {
    private final String name = "SETEX";
    private KeyValueStore cache;

    public SETEXCommand(KeyValueStore cache) {
        this.cache = cache;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public RespValue execute(String[] args) {
        if(args == null || args.length != 4){
            return new RespErr("ERR", "wrong number of arguments for '" + name + "' command");
        }

        int seconds;

        try {
            seconds = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            return new RespErr("ERR", "invalid expire time in '" + name + "' command");
        }

        if(seconds < 0) {
            return new RespErr("ERR", "invalid expire time in '" + name + "' command");
        }

        cache.put(args[1], new StringValue(args[3]), TimeUnit.SECONDS.toMillis(seconds));

        return new RespSimpleString("OK");
    }
}
