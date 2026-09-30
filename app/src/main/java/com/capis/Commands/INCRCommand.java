package com.capis.Commands;

import com.capis.DataTpes.Core.StringValue;
import com.capis.DataTpes.RespValues.RespErr;
import com.capis.DataTpes.RespValues.RespInteger;
import com.capis.DataTpes.RespValues.RespValue;
import com.capis.entities.KeyValueStore;

public class INCRCommand implements Command {
    private KeyValueStore cache;

    private String name = "INCR";

    public INCRCommand(KeyValueStore cache) {
        this.cache = cache;
    }

    public String getName() {
        return name;
    }

    public RespValue execute(String[] args) {
        // Implementation for INCR command
        // Increment the value associated with the key in the cache
        // Return the new value as a RespInteger
        
        if(args == null || args.length != 2) {
            // Handle error: wrong number of arguments
            return new RespErr("ERR", "wrong number of arguments for 'incr' command");
        }

        String key = args[1];

        try {
            cache.increment(key);
            String value = ((StringValue) cache.get(key)).getValue();
            return new RespInteger(Integer.parseInt(value));
        } catch (NumberFormatException e) {
            return new RespErr("ERR", "value is not an integer or out of range");
        }
    }
}
