package com.capis.Commands;

import com.capis.DataTpes.Core.StringValue;
import com.capis.DataTpes.RespValues.RespErr;
import com.capis.DataTpes.RespValues.RespSimpleString;
import com.capis.DataTpes.RespValues.RespValue;
import com.capis.entities.KeyValueStore;

public class SetCommand implements Command {
    private final String name = "SET";
    private final KeyValueStore keyValueStore;

    public SetCommand(KeyValueStore keyValueStore) {
        this.keyValueStore = keyValueStore;
    }

    @Override   
    public String getName() {
        return name;
    }

    @Override
    public RespValue execute(String[] args) {
        if (args.length != 3) {
            return new RespErr("ERR", "SET command requires exactly two arguments.");
        }
        String key = args[1];
        String value = args[2];
        keyValueStore.put(key, new StringValue(value));
        return new RespSimpleString("OK");
    }
}
