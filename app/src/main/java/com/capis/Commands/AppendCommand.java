package com.capis.Commands;

import java.nio.charset.StandardCharsets;

import com.capis.DataTypes.Core.StringValue;
import com.capis.DataTypes.Core.Value;
import com.capis.DataTypes.RespValues.RespErr;
import com.capis.DataTypes.RespValues.RespInteger;
import com.capis.DataTypes.RespValues.RespValue;
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

        return new RespInteger(appended.getBytes(StandardCharsets.UTF_8).length);
    }
}
