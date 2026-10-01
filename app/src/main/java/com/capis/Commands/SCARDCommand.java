package com.capis.Commands;

import com.capis.DataTpes.Core.SetValue;
import com.capis.DataTpes.Core.Value;
import com.capis.DataTpes.RespValues.RespErr;
import com.capis.DataTpes.RespValues.RespInteger;
import com.capis.DataTpes.RespValues.RespValue;
import com.capis.entities.KeyValueStore;

public class SCARDCommand implements Command {
    private final String name = "SCARD";
    private final KeyValueStore cache;

    public SCARDCommand(KeyValueStore cache) {
        this.cache = cache;
    }

    public String getName() {
        return name;
    }

    public RespValue execute(String[] args){
        if(args == null || args.length != 2){
            return new RespErr("ERR", "wrong number of arguments for 'SCARD' command");
        }

        Value<?> existing = cache.get(args[1]);

        if (existing == null) {
            return new RespInteger(0);
        }

        if (!(existing instanceof SetValue setValue)) {
            return new RespErr(
                "WRONGTYPE",
                "Operation against a key holding the wrong kind of value"
            );
        }

        return new RespInteger(
            setValue.getValue().size()
        );
    }
}
