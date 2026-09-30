package com.capis.Commands;

import com.capis.DataTpes.Core.StringValue;
import com.capis.DataTpes.RespValues.RespErr;
import com.capis.DataTpes.RespValues.RespSimpleString;
import com.capis.DataTpes.RespValues.RespValue;
import com.capis.entities.KeyValueStore;

public class MSetCommand implements Command {
    String name = "MSET";
    KeyValueStore cache;

    public MSetCommand(KeyValueStore cache) {
        this.cache = cache;
    }

    @Override
    public String getName() {
        return name;
    }

    public RespValue execute(String[] args) {
        // Implementation for MSET command
        // For each key-value pair in args, set the key to the value
        if (args == null || args.length < 3 || args.length % 2 == 0) {
            return new RespErr(
                "ERR",
                "wrong number of arguments for 'mset' command"
            );
        }

        for (int i = 1; i+1 < args.length; i += 2) {
            String key = args[i];
            String value = args[i + 1];

            if(key == null || value == null || key.isEmpty() || value.isEmpty()) {
                return new RespErr("ERR", "wrong number of arguments for 'mset' command\r\n");
            }

            cache.put(key, new StringValue(value));
        }
        
        return new RespSimpleString("OK"); // Placeholder return value
    }
}
