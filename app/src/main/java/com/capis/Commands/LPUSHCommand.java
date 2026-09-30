package com.capis.Commands;

import com.capis.DataTpes.Core.ListValue;
import com.capis.DataTpes.RespValues.RespErr;
import com.capis.DataTpes.RespValues.RespInteger;
import com.capis.DataTpes.RespValues.RespValue;
import com.capis.entities.KeyValueStore;

public class LPUSHCommand implements Command {

    private final String name = "LPUSH";
    private final KeyValueStore cache;

    public LPUSHCommand(KeyValueStore cache) {
        this.cache = cache;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public RespValue execute(String[] args) {

        if (args == null || args.length < 3) {
            return new RespErr(
                "ERR",
                "wrong number of arguments for 'lpush' command"
            );
        }

        String key = args[1];

        if (cache.containsKey(key)
                && !(cache.get(key) instanceof ListValue)) {

            return new RespErr(
                "WRONGTYPE",
                "Operation against a key holding the wrong kind of value"
            );
        }

        ListValue listValue;

        if (cache.containsKey(key)) {
            listValue = (ListValue) cache.get(key);
        } else {
            listValue = new ListValue(new java.util.ArrayList<>());
            cache.put(key, listValue);
        }

        for (int i = 2; i < args.length; i++) {

            if (args[i] == null) {
                return new RespErr(
                    "ERR",
                    "invalid argument"
                );
            }

            listValue.getValue().add(0, args[i]);
        }

        return new RespInteger(listValue.getValue().size());
    }
}