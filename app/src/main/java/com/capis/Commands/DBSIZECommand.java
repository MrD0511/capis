package com.capis.Commands;

import com.capis.DataTpes.RespValues.RespErr;
import com.capis.DataTpes.RespValues.RespInteger;
import com.capis.DataTpes.RespValues.RespValue;
import com.capis.entities.KeyValueStore;

public class DBSIZECommand implements Command {
    private final String commandName = "DBSIZE";
    private KeyValueStore cache;

    public DBSIZECommand(KeyValueStore cache) {
        this.cache = cache;
    }

    @Override
    public String getName() {
        return commandName;
    }

    @Override
    public RespValue execute(String[] args) {
        if(args == null || args.length != 1){
            return new RespErr("ERR", "wrong number of arguments for '" + commandName + "' command");
        }

        return new RespInteger(cache.size());
    }
}