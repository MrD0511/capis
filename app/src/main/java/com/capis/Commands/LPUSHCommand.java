package com.capis.Commands;

import java.util.ArrayList;
import java.util.List;

import com.capis.DataTpes.Core.ListValue;
import com.capis.DataTpes.Core.Value;
import com.capis.DataTpes.RespValues.RespErr;
import com.capis.DataTpes.RespValues.RespInteger;
import com.capis.DataTpes.RespValues.RespValue;
import com.capis.entities.KeyValueStore;

public class LPUSHCommand implements Command {

    private final String name = "LPUSH";
    private final KeyValueStore cache;

    public LPUSHCommand(KeyValueStore cache) {
        this.cache = cache;
    }

    @Override
    public String getName() {
        return name;
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
            listValue = new ListValue(new ArrayList<>());
            cache.put(key, listValue);
        }else if(existing instanceof ListValue existingList){
            listValue = existingList;
        }else{
            return new RespErr(
                "ERR",
                "wrong type of value for 'lpush' command"
            );
        }

        List<String> list = listValue.getValue();


        for (int i = 2; i < args.length; i++) {

            if (args[i] == null) {
                return new RespErr(
                    "ERR",
                    "invalid argument"
                );
            }

            list.add(0, args[i]);
        }

        return new RespInteger(list.size());
    }
}