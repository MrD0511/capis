package com.capis.Commands;

import com.capis.DataTpes.Core.StringValue;
import com.capis.DataTpes.RespValues.RespErr;
import com.capis.DataTpes.RespValues.RespSimpleString;
import com.capis.DataTpes.RespValues.RespValue;
import com.capis.entities.KeyValueStore;

public class SetCommand implements Command {
    String name = "SET";
    private KeyValueStore keyValueStore;

    public SetCommand(KeyValueStore keyValueStore) {
        this.keyValueStore = keyValueStore;
    }

    public String getName() {
        return name;
    }

    public RespValue execute(String[] args) {
        if (args.length != 3) {
            return new RespErr("ERR", "SET command requires exactly two arguments.\r\n");
        }
        String key = args[1];
        String value = args[2];
        keyValueStore.put(key, new StringValue(value));
        return new RespSimpleString("OK");
    }
}
