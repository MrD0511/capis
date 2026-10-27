package com.capis.Commands;

import com.capis.DataTpes.RespValues.RespErr;
import com.capis.DataTpes.RespValues.RespInteger;
import com.capis.DataTpes.RespValues.RespValue;
import com.capis.entities.KeyValueStore;

public class DELCommand implements Command {
    private final String name = "DEL";
    private final KeyValueStore cache;

    public DELCommand(KeyValueStore cache) {
        this.cache = cache;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public RespValue execute(String[] args) {
        // Implementation for DEL command
        // For each key in args, delete the key from the cache
        if (args == null || args.length < 2) {
            // Handle error: wrong number of arguments
            return new RespErr("ERR", "wrong number of arguments for 'DEL' command");
        }

        int count = 0;
        for (int i = 1; i < args.length; i++) {
            String key = args[i];
            if (cache.remove(key) != null) {
                count++;
            }
        }

        return new RespInteger(count);
    }
}
