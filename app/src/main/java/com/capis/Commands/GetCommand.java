package com.capis.Commands;

import com.capis.DataTpes.Core.StringValue;
import com.capis.DataTpes.Core.Value;
import com.capis.DataTpes.RespValues.RespBulkString;
import com.capis.DataTpes.RespValues.RespErr;
import com.capis.DataTpes.RespValues.RespValue;
import com.capis.entities.KeyValueStore;

public class GetCommand implements Command {
    private final KeyValueStore cache;
    private final String name = "GET";

    public GetCommand(KeyValueStore cache) {
        this.cache = cache;
    }
    
    @Override
    public String getName() {
        return name;
    }

    @Override
    public RespValue execute(String[] args) {
        if (args == null || args.length != 2) {
            return new RespErr("ERR", "wrong number of arguments for 'get' command.");
        }

        String key = args[1];
        Value<?> value = cache.get(key);

        if (value == null) {
            return null;
        }

        if (value instanceof StringValue stringValue) {
            return new RespBulkString(stringValue.getValue());
        }

        return new RespErr("WRONGTYPE", "Operation against a key holding the wrong kind of value.");
    }
}
