package com.capis.Commands;

import com.capis.DataTypes.Core.HashValue;
import com.capis.DataTypes.Core.ListValue;
import com.capis.DataTypes.Core.SetValue;
import com.capis.DataTypes.Core.StringValue;
import com.capis.DataTypes.Core.Value;
import com.capis.DataTypes.RespValues.RespErr;
import com.capis.DataTypes.RespValues.RespSimpleString;
import com.capis.DataTypes.RespValues.RespValue;
import com.capis.entities.KeyValueStore;

public class TypeCommand implements Command {
    private final String name = "TYPE";
    private final KeyValueStore keyValueStore;

    public TypeCommand(KeyValueStore keyValueStore) {
        this.keyValueStore = keyValueStore;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override 
    public RespValue execute(String[] args) {
        if(args == null || args.length != 2) {
            return new RespErr("ERR", "wrong number of arguments for 'type' command");
        }

        String key = args[1];
        Value<?> value = keyValueStore.get(key);

        if(value == null) {
            return new RespSimpleString("none");
        }

        if(value instanceof StringValue){
            return new RespSimpleString("string");
        }
        
        if(value instanceof ListValue){
            return new RespSimpleString("list");
        }
        
        if(value instanceof SetValue){
            return new RespSimpleString("set");
        }
        
        if(value instanceof HashValue){
            return new RespSimpleString("hash");
        }
        
        return new RespSimpleString("zset"); 
        
    }

}
