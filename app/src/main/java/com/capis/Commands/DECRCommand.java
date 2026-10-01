package com.capis.Commands;

import com.capis.DataTpes.Core.Value;
import com.capis.DataTpes.RespValues.RespErr;
import com.capis.DataTpes.RespValues.RespInteger;
import com.capis.DataTpes.RespValues.RespValue;
import com.capis.entities.KeyValueStore;

public class DECRCommand implements Command{
    KeyValueStore cache;

    String name = "DECR";

    public DECRCommand(KeyValueStore cache) {
        this.cache = cache;
    }

    public String getName() {
        return name;
    }

    public RespValue execute(String[] args) {
        if (args == null || args.length != 2) {
            return new RespErr(
                "ERR",
                "wrong number of arguments for 'DECR' command"
            );
        }

        String key = args[1];

        try {
            Value<?> value = cache.decrement(key);

            return new RespInteger(
                Integer.parseInt((String) value.getValue())
            );

        } catch (IllegalArgumentException e) {
            return new RespErr(
                "ERR",
                "value is not an integer or out of range"
            );
        }
    }
}
