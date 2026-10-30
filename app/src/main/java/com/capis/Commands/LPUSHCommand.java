package com.capis.Commands;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import com.capis.DataTypes.Core.ListValue;
import com.capis.DataTypes.Core.Value;
import com.capis.DataTypes.RespValues.RespArray;
import com.capis.DataTypes.RespValues.RespBulkString;
import com.capis.DataTypes.RespValues.RespErr;
import com.capis.DataTypes.RespValues.RespInteger;
import com.capis.DataTypes.RespValues.RespValue;
import com.capis.entities.KeyValueStore;

public class LPUSHCommand implements Command {

    private final String name = "LPUSH";
    private final KeyValueStore cache;
    private final boolean changesState = true;

    public LPUSHCommand(KeyValueStore cache) {
        this.cache = cache;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override 
    public boolean changesState() {
        return changesState;
    }

    @Override
    public RespValue execute(String[] args) {

        if (args == null || args.length < 3) {
            return new RespErr(
                "ERR",
                "wrong number of arguments for 'lpush' command"
            );
        }

        String key = args[1];

        Value<?> existing = cache.get(key);
        
        ListValue listValue;
        
        if(existing == null){
            listValue = new ListValue(new ArrayDeque<>());
            cache.put(key, listValue);
        } else if(existing instanceof ListValue listValue1 ){
            listValue = listValue1;
        }else{
            return new RespErr(
                "ERR",
                "wrong type of value for 'lpush' command"
            );
        }

        Deque<String> list = listValue.getValue();


        for (int i = 2; i < args.length; i++) {

            if (args[i] == null) {
                return new RespErr(
                    "ERR",
                    "invalid argument"
                );
            }

            list.offerFirst(args[i]);
        }

        return new RespInteger(list.size());
    }

    @Override 
    public RespValue persistentCommand(String[] args) {
        if (args == null || args.length < 3) {
            return new RespErr(
                "ERR",
                "wrong number of arguments for 'lpush' command"
            );
        }

        List<RespValue> respValues = new ArrayList<>();

        respValues.add(new RespBulkString("LPUSH"));

        for (int i = 1; i < args.length; i++) {
            respValues.add(new RespBulkString(args[i]));
        }

        return new RespArray(respValues);
    }
}