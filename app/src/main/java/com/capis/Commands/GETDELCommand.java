package com.capis.Commands;

import com.capis.DataTypes.Core.StringValue;
import com.capis.DataTypes.Core.Value;
import com.capis.DataTypes.RespValues.RespBulkString;
import com.capis.DataTypes.RespValues.RespErr;
import com.capis.DataTypes.RespValues.RespValue;
import com.capis.entities.KeyValueStore;

public class GETDELCommand implements Command {
    private final String name = "GETDEL";
    private final KeyValueStore cache;

    public GETDELCommand(KeyValueStore cache) {
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

        Value<?> existing = cache.get(args[1]);

        if(existing == null){
            return null; 
        }

        if(!(existing instanceof StringValue stringValue)){
            return new RespErr("ERR", "WRONGTYPE Operation against a key holding the wrong kind of value");
        }

        cache.remove(args[1]);

        return new RespBulkString(stringValue.getValue());
    }
    
}
