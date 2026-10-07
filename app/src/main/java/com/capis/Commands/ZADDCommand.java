package com.capis.Commands;

import java.util.TreeSet;

import com.capis.DataTpes.Core.ScoreMember;
import com.capis.DataTpes.Core.SortedSetValue;
import com.capis.DataTpes.Core.Value;
import com.capis.DataTpes.RespValues.RespErr;
import com.capis.DataTpes.RespValues.RespInteger;
import com.capis.DataTpes.RespValues.RespValue;
import com.capis.entities.KeyValueStore;

public class ZADDCommand implements Command {
    private final String name = "ZADD";
    private KeyValueStore cache;

    public ZADDCommand(KeyValueStore cache) {
        this.cache = cache;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public RespValue execute(String[] args) {
        // Implementation for ZADD command goes here
        if(args == null || args.length < 4 || args.length % 2 != 0) {
            return new RespErr("ERR", "wrong number of arguments for '" + name + "' command");
        }

        Value<?> existing = cache.get(args[1]);

        SortedSetValue zset;
        
        if(existing == null) {
            zset = new SortedSetValue(new TreeSet<>());
            cache.put(args[1], zset);
        } else if(existing instanceof SortedSetValue existingZSet) {
            zset = existingZSet;
        } else {
            return new RespErr("ERR", "WRONGTYPE Operation against a key holding the wrong kind of value");
        }

        int added = 0;

        for(int i = 2; i + 1 < args.length; i += 2) {
            double score;
            try {
                score = Double.parseDouble(args[i]);
            } catch (NumberFormatException e) {
                return new RespErr("ERR", "value is not a valid float");
            }

            if(zset.find(args[i + 1]) == null) {
                added++;
            }

            zset.removeMember(args[i + 1]);
            zset.getValue().add(new ScoreMember(score, args[i + 1]));
        }

        return new RespInteger(added); // Placeholder return statement
    }
}