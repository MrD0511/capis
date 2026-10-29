package com.capis.Commands;

import java.util.Arrays;

import com.capis.DataTypes.RespValues.RespArray;
import com.capis.DataTypes.RespValues.RespBulkString;
import com.capis.DataTypes.RespValues.RespErr;
import com.capis.DataTypes.RespValues.RespInteger;
import com.capis.DataTypes.RespValues.RespValue;
import com.capis.entities.KeyValueStore;

public class PERSISTCommand implements Command {
    private final String name = "PERSIST";
    private final KeyValueStore cache;
    private final boolean changesState = true;

    public PERSISTCommand(KeyValueStore cache) {
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
        if(args == null || args.length != 2){
            return new RespErr("ERR", "wrong number of arguments for '" + name + "' command");
        }

        boolean result = cache.persist(args[1]);
        
        return new RespInteger(result ? 1 : 0);
    }

    @Override
    public RespValue persistentCommand(String[] args) {
        if(args == null || args.length != 2){
            return new RespErr("ERR", "wrong number of arguments for '" + name + " ' command");
        }

        return new RespArray(
            Arrays.asList(
                new RespBulkString(name),
                new RespBulkString(args[1])
            )
        );
    }
}