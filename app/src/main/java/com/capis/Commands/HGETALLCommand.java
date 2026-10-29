package com.capis.Commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.capis.DataTypes.Core.HashValue;
import com.capis.DataTypes.Core.Value;
import com.capis.DataTypes.RespValues.RespArray;
import com.capis.DataTypes.RespValues.RespBulkString;
import com.capis.DataTypes.RespValues.RespErr;
import com.capis.DataTypes.RespValues.RespValue;
import com.capis.entities.KeyValueStore;

public class HGETALLCommand implements Command {
    private final String name = "HGETALL";
    private final KeyValueStore cache;

    public HGETALLCommand(KeyValueStore cache) {
        this.cache = cache;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public RespValue execute(String[] args) {
        if(args == null || args.length != 2){
            return new RespErr("ERR", "wrong number of arguments for '" + name + "' command");
        }

        Value<?> existing = cache.get(args[1]);
        
        if(existing == null){
            return new RespArray(List.of());
        }

        if(!(existing instanceof HashValue hash)){
            return new RespErr("ERR", "WRONGTYPE Operation against a key holding the wrong kind of value");
        }

        List<RespValue> result = new ArrayList<>();

        for(Map.Entry<String, String> entry: hash.getValue().entrySet()){
            result.add(new RespBulkString(entry.getKey()));
            result.add(new RespBulkString(entry.getValue()));
        }

        return new RespArray(result);
    }
}
