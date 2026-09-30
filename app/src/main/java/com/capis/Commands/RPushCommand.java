package com.capis.Commands;

import java.util.ArrayList;
import java.util.List;

import com.capis.DataTpes.Core.ListValue;
import com.capis.DataTpes.Core.Value;
import com.capis.DataTpes.RespValues.RespErr;
import com.capis.DataTpes.RespValues.RespInteger;
import com.capis.DataTpes.RespValues.RespValue;
import com.capis.entities.KeyValueStore;

public class RPushCommand implements Command {

    private final String name = "RPUSH";
    private final KeyValueStore cache;

    public RPushCommand(KeyValueStore cache) {
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
                "wrong number of arguments for 'rpush' command"
            );
        }

        String key = args[1];

        Value<?> existingValue = cache.get(key);

        ListValue listValue;

        if (existingValue == null) {
            listValue = new ListValue(new ArrayList<>());
            cache.put(key, listValue);
        } else if (existingValue instanceof ListValue existingList) {
            listValue = existingList;
        } else {
            return new RespErr(
                "WRONGTYPE",
                "Operation against a key holding the wrong kind of value"
            );
        }

        List<String> list = listValue.getValue();

        for (int i = 2; i < args.length; i++) {
            list.add(args[i]);
        }

        return new RespInteger(list.size());
    }
}