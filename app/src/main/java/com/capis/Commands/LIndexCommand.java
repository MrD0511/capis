package com.capis.Commands;

import java.util.List;

import com.capis.DataTpes.Core.ListValue;
import com.capis.DataTpes.Core.Value;
import com.capis.DataTpes.RespValues.RespBulkString;
import com.capis.DataTpes.RespValues.RespErr;
import com.capis.DataTpes.RespValues.RespValue;
import com.capis.entities.KeyValueStore;

public class LIndexCommand implements Command {

    private final String name = "LINDEX";
    private final KeyValueStore cache;

    public LIndexCommand(KeyValueStore cache) {
        this.cache = cache;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public RespValue execute(String[] args) {

        if (args == null || args.length != 3) {
            return new RespErr(
                "ERR",
                "wrong number of arguments for 'lindex' command"
            );
        }

        int index;

        try {
            index = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            return new RespErr(
                "ERR",
                "value is not an integer or out of range"
            );
        }

        Value<?> value = cache.get(args[1]);

        if (value == null) {
            return null;
        }

        if (!(value instanceof ListValue listValue)) {
            return new RespErr(
                "WRONGTYPE",
                "Operation against a key holding the wrong kind of value"
            );
        }

        List<String> list = listValue.getValue();

        if (index < 0) {
            index = list.size() + index;
        }

        if (index < 0 || index >= list.size()) {
            return null;
        }

        return new RespBulkString(list.get(index));
    }
}