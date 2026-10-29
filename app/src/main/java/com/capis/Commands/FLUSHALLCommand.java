package com.capis.Commands;

import java.util.Arrays;

import com.capis.DataTypes.RespValues.RespArray;
import com.capis.DataTypes.RespValues.RespErr;
import com.capis.DataTypes.RespValues.RespSimpleString;
import com.capis.DataTypes.RespValues.RespValue;
import com.capis.entities.KeyValueStore;

public class FLUSHALLCommand implements Command {
    private final String name = "FLUSHALL";
    private final KeyValueStore cache;
    private final boolean changesState = true;

    public FLUSHALLCommand(KeyValueStore cache) {
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
        if(args == null || args.length != 1){
            return new RespErr("ERR", "wrong number of arguments for '" + name + "' command");
        }

        cache.clear();
        
        return new RespSimpleString("OK");
    }

    @Override
    public RespValue persistentCommand(String[] args) {
        if(args == null || args.length != 1){
            return new RespErr("ERR", "wrong number of arguments for '" + name + "' command");
        }
   
        return new RespArray(
            Arrays.asList(
                new RespSimpleString(name)
            )
        );
    }
}
