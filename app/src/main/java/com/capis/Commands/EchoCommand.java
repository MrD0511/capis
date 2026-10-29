package com.capis.Commands;

import com.capis.DataTypes.RespValues.RespBulkString;
import com.capis.DataTypes.RespValues.RespErr;
import com.capis.DataTypes.RespValues.RespSimpleString;
import com.capis.DataTypes.RespValues.RespValue;

public class EchoCommand implements Command {
    String name = "ECHO";

    @Override
    public String getName() {
        return name;
    }

    @Override
    public RespValue execute(String[] args) {
        return switch (args.length) {
            case 1 -> new RespSimpleString("ECHO");
            case 2 -> new RespBulkString(args[1]);
            default -> new RespErr("ERR", "ECHO command takes at most one argument.\r\n");
        };
    }
}
