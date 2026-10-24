package com.capis.Commands;

import java.util.ArrayList;
import java.util.Arrays;

import com.capis.DataTpes.RespValues.RespArray;
import com.capis.DataTpes.RespValues.RespBulkString;
import com.capis.DataTpes.RespValues.RespErr;
import com.capis.DataTpes.RespValues.RespInteger;
import com.capis.DataTpes.RespValues.RespValue;

public class HELLOCommand implements Command {
    private final String commandName = "HELLO";

    @Override
    public String getName() {
        return commandName;
    }

    @Override
    public RespValue execute(String[] args) {
        if (args.length > 1 && !args[1].equals("2")) {
            return new RespErr("NOPROTO", "sorry, this protocol version is not supported");
        }

        return new RespArray(Arrays.asList(
            new RespBulkString("server"),  new RespBulkString("capis"),
            new RespBulkString("version"), new RespBulkString("1.0.0"),
            new RespBulkString("proto"),   new RespInteger(2),
            new RespBulkString("id"),      new RespInteger(1),
            new RespBulkString("mode"),    new RespBulkString("standalone"),
            new RespBulkString("role"),    new RespBulkString("master"),
            new RespBulkString("modules"), new RespArray(new ArrayList<RespValue>())
        ));
    }
}
