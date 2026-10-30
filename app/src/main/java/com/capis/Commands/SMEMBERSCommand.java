package com.capis.Commands;

import java.util.ArrayList;
import java.util.List;

import com.capis.DataTypes.Core.SetValue;
import com.capis.DataTypes.Core.Value;
import com.capis.DataTypes.RespValues.RespArray;
import com.capis.DataTypes.RespValues.RespBulkString;
import com.capis.DataTypes.RespValues.RespErr;
import com.capis.DataTypes.RespValues.RespValue;
import com.capis.entities.KeyValueStore;

public class SMEMBERSCommand implements Command {
    private final String name = "SMEMBERS";
    private final KeyValueStore cache;

    public SMEMBERSCommand(KeyValueStore cache) {
        this.cache = cache;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public RespValue execute(String[] args){
        if(args == null || args.length != 2){
            return new RespErr("ERR", "wrong number of arguments for 'SMEMBERS' command");
        }

        Value<?> existing = cache.get(args[1]);

        if (existing == null) {
            return new RespArray(List.of());
        }

        if (!(existing instanceof SetValue setValue)) {
            return new RespErr(
                "WRONGTYPE",
                "Operation against a key holding the wrong kind of value"
            );
        }

        List<RespValue> result = new ArrayList<>();

        for (String member : setValue.getValue()) {
            result.add(
                new RespBulkString(member)
            );
        }

        return new RespArray(result);
    }
}
