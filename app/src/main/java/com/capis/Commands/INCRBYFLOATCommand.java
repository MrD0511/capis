package com.capis.Commands;

import com.capis.DataTpes.Core.StringValue;
import com.capis.DataTpes.Core.Value;
import com.capis.DataTpes.RespValues.RespBulkString;
import com.capis.DataTpes.RespValues.RespErr;
import com.capis.DataTpes.RespValues.RespValue;
import com.capis.entities.KeyValueStore;

public class INCRBYFLOATCommand implements Command {
    private final String name = "INCRBYFLOAT";
    private KeyValueStore cache;

    public INCRBYFLOATCommand(KeyValueStore cache) {
        this.cache = cache;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public RespValue execute(String[] args) {
        if(args == null || args.length != 3){
            return new RespErr("ERR", "wrong number of arguments for '" + name + "' command");
        }

        double delta;

        try{
            delta = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            return new RespErr("ERR", "value is not a valid float");
        }

        Value<?> existing = cache.get(args[1]);



        if(existing != null && !(existing instanceof StringValue)) {
            return new RespErr("ERR", "WRONGTYPE Operation against a key holding the wrong kind of value");
        }

        double current = 0;

        if (existing instanceof StringValue stringValue) {
            try {
                current = Double.parseDouble(stringValue.getValue());
            } catch (NumberFormatException e) {
                return new RespErr("ERR", "value is not a valid float");
            }
        }

        double result = current + delta;

        if(Double.isInfinite(result) || Double.isNaN(result)) {
            return new RespErr("ERR", "increment would produce NaN or Infinity");
        }


        String formatted = formatScore(result);
        StringValue target = (StringValue) existing;

        if(target == null) {
            target = new StringValue(formatted);
            cache.put(args[1], target);
        } else {
            target.setValue(formatted);
        }

        return new RespBulkString(formatted);
    }

    private static String formatScore(double score) {
        if (!Double.isInfinite(score) && score == Math.floor(score)) {
            return Long.toString((long) score);
        }
        return Double.toString(score);
    }
}
