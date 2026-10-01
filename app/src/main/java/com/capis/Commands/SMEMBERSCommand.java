package com.capis.Commands;

import java.util.ArrayList;
import java.util.List;

import com.capis.DataTpes.Core.SetValue;
import com.capis.DataTpes.Core.Value;
import com.capis.DataTpes.RespValues.RespArray;
import com.capis.DataTpes.RespValues.RespBulkString;
import com.capis.DataTpes.RespValues.RespErr;
import com.capis.DataTpes.RespValues.RespValue;
import com.capis.entities.KeyValueStore;

public class SMEMBERSCommand implements Command {
    private final String name = "SMEMBERS";
    private KeyValueStore cache;

    public SMEMBERSCommand(KeyValueStore cache) {
        this.cache = cache;
    }

    public String getName() {
        return name;
    }

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
