package com.capis.Commands;

import com.capis.DataTpes.Core.ListValue;
import com.capis.DataTpes.Core.Value;
import com.capis.DataTpes.RespValues.RespErr;
import com.capis.DataTpes.RespValues.RespInteger;
import com.capis.DataTpes.RespValues.RespValue;
import com.capis.entities.KeyValueStore;

public class LLENCommand implements Command {

    private final String name = "LLEN";
    private final KeyValueStore cache;

    public LLENCommand(KeyValueStore cache) {
        this.cache = cache;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public RespValue execute(String[] args) {

        if (args == null || args.length != 2) {
            return new RespErr(
                "ERR",
                "wrong number of arguments for 'llen' command"
            );
        }

        Value<?> value = cache.get(args[1]);

        if (value == null) {
            return new RespInteger(0);
        }

        if (!(value instanceof ListValue listValue)) {
            return new RespErr(
                "WRONGTYPE",
                "Operation against a key holding the wrong kind of value"
            );
        }

        return new RespInteger(listValue.getValue().size());
    }
}