package com.capis.Commands;

import com.capis.DataTpes.Core.StringValue;
import com.capis.DataTpes.Core.Value;
import com.capis.DataTpes.RespValues.RespBulkString;
import com.capis.DataTpes.RespValues.RespErr;
import com.capis.DataTpes.RespValues.RespValue;
import com.capis.entities.KeyValueStore;

public class GetCommand implements Command {
    private KeyValueStore cache;
    String name = "GET";

    public GetCommand(KeyValueStore cache) {
        this.cache = cache;
    }

    public String getName() {
        return name;
    }

    public RespValue execute(String[] args) {
        if (args == null || args.length != 2) {
            return new RespErr("ERR", "wrong number of arguments for 'get' command\r\n");
        }

        String key = args[1];
        Value<?> value = cache.get(key);

        if (value == null) {
            return null;
        }

        if (value instanceof StringValue) {
            return new RespBulkString(((StringValue) value).getValue());
        }

        return new RespErr("WRONGTYPE", "Operation against a key holding the wrong kind of value\r\n");
    }
}
