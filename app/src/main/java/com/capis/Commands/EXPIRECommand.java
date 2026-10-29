package com.capis.Commands;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.capis.DataTypes.RespValues.RespArray;
import com.capis.DataTypes.RespValues.RespBulkString;
import com.capis.DataTypes.RespValues.RespErr;
import com.capis.DataTypes.RespValues.RespInteger;
import com.capis.DataTypes.RespValues.RespValue;
import com.capis.entities.KeyValueStore;

public class EXPIRECommand implements Command {
    private final String name = "EXPIRE";
    private final KeyValueStore cache;
    private final boolean changesState = true;

    public EXPIRECommand(KeyValueStore cache) {
        this.cache = cache;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean changesState() {
        return changesState;
    }

    @Override 
    public RespValue execute(String[] args) {
        if(args == null || args.length != 3){
            return new RespErr("ERR", "wrong number of arguments for '" + name + "' command");
        }

        int seconds;

        try {
            seconds = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            return new RespErr("ERR", "value is not an integer or out of range");
        }

        if(seconds < 0) {
            return new RespErr("ERR", "invalid expire time in '" + name + "' command");
        }

        return new RespInteger(
                cache.expire(args[1], TimeUnit.SECONDS.toMillis(seconds)) ? 1 : 0
            );
    }

    @Override
    public RespValue persistentCommand(String[] args) {
        if(args == null || args.length != 3){
            return new RespErr("ERR", "wrong number of arguments for '" + name + "command");
        }

        int seconds;

        try {
            seconds = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            return new RespErr("ERR", "value is not an integer or out of range");
        }

        if(seconds < 0) {
            return new RespErr("ERR", "invalid expire time in '" + name + "' command");
        }

        return new RespArray(
            Arrays.asList(
                new RespBulkString(name),
                new RespBulkString(args[1]),
                new RespBulkString(args[2])
            )
        );
    }
}
