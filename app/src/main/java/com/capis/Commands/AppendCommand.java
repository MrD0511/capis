package com.capis.Commands;

import com.capis.DataTpes.Core.StringValue;
import com.capis.DataTpes.Core.Value;
import com.capis.DataTpes.RespValues.RespErr;
import com.capis.DataTpes.RespValues.RespInteger;
import com.capis.DataTpes.RespValues.RespValue;
import com.capis.entities.KeyValueStore;

public class AppendCommand implements Command {
    private final String name = "APPEND";
    private KeyValueStore cache;
 
    public AppendCommand(KeyValueStore cache) {
        this.cache = cache;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public RespValue execute(String[] args) {
        if(args == null || args.length != 3){
            return new RespErr("ERR", "wrong number of arguments for '" + name + "' command");
        }

        Value<?> existing = cache.get(args[1]);

        if(existing == null){
            cache.put(args[1], new StringValue(args[2]));
            return new RespInteger(args[2].length());
        }

        if(!(existing instanceof StringValue stringValue)){
            return new RespErr("ERR", "WRONGTYPE Operation against a key holding the wrong kind of value");
        }

        String appended = stringValue.getValue() + args[2];

        stringValue.setValue(appended);

        return new RespInteger(appended.length());
    }
}
