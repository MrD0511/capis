package com.capis.Commands;

import java.nio.charset.StandardCharsets;

import com.capis.DataTpes.Core.StringValue;
import com.capis.DataTpes.Core.Value;
import com.capis.DataTpes.RespValues.RespErr;
import com.capis.DataTpes.RespValues.RespInteger;
import com.capis.DataTpes.RespValues.RespValue;
import com.capis.entities.KeyValueStore;

public class STRLENCommand implements Command {
    private final String name = "STRLEN";
    private KeyValueStore cache;

    public STRLENCommand(KeyValueStore cache) {
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
            return new RespInteger(0);
        }

        if(!(existing instanceof StringValue stringValue)){
            return new RespErr("ERR", "WRONGTYPE Operation against a key holding the wrong kind of value");
        }

        return new RespInteger(stringValue.getValue().getBytes(StandardCharsets.UTF_8).length);
    }
}
