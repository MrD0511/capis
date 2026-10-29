package com.capis.Commands;

import java.util.ArrayList;
import java.util.List;

import com.capis.DataTypes.Core.StringValue;
import com.capis.DataTypes.Core.Value;
import com.capis.DataTypes.RespValues.RespArray;
import com.capis.DataTypes.RespValues.RespBulkString;
import com.capis.DataTypes.RespValues.RespErr;
import com.capis.DataTypes.RespValues.RespValue;
import com.capis.entities.KeyValueStore;

public class MGetCommand implements Command {
    private final KeyValueStore cache;
    private final String name = "MGET";

    public MGetCommand(KeyValueStore cache) {
        this.cache = cache;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public RespValue execute(String[] args) {
        if (args == null || args.length < 2) {
            return new RespErr(
                "ERR",
                "wrong number of arguments for 'mget' command"
            );
        }

        List<RespValue> values = new ArrayList<>();

        for(int i = 1; i < args.length; i++) {
            String key = args[i];
            if(key == null || key.isEmpty()) {
                return new RespErr("ERR", "wrong number of arguments for 'mget' command");
            }

            Value<?> value = cache.get(key);

            if(value == null || !(value instanceof StringValue)) {
                values.add(null);
            }else{
                values.add(new RespBulkString(((StringValue) value).getValue()));
            }
        }

        // Process the MGET command
        // For each key, retrieve the value from the key-value store
        // Return a list of values or null if a key is not found
        return new RespArray(values);
    }
}
