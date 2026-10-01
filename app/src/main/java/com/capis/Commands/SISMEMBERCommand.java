package com.capis.Commands;

import com.capis.DataTpes.Core.SetValue;
import com.capis.DataTpes.Core.Value;
import com.capis.DataTpes.RespValues.RespErr;
import com.capis.DataTpes.RespValues.RespInteger;
import com.capis.DataTpes.RespValues.RespValue;
import com.capis.entities.KeyValueStore;

public class SISMEMBERCommand implements Command {
    private final String name = "SISMEMBER";
    private  KeyValueStore cache;

    public SISMEMBERCommand(KeyValueStore cache) {
        this.cache = cache;
    }

    public String getName() {
        return name;
    }

    public RespValue execute(String[] args) {
        if (args == null || args.length != 3) {
            return new RespErr("ERR", "wrong number of arguments for 'SISMEMBER' command");
        }

        String key = args[1];
        String member = args[2];

        Value<?> existing = cache.get(key);

        if (existing == null) {
            return new RespInteger(0);
        }

        if (!(existing instanceof SetValue setValue)) {
            return new RespErr(
                "WRONGTYPE",
                "Operation against a key holding the wrong kind of value"
            );
        }

        boolean isMember = setValue.getValue().contains(member);
        return new RespInteger(isMember ? 1 : 0);
    }
}
