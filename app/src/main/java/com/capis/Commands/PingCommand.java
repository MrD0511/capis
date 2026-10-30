package com.capis.Commands;

import com.capis.DataTypes.RespValues.RespBulkString;
import com.capis.DataTypes.RespValues.RespErr;
import com.capis.DataTypes.RespValues.RespSimpleString;
import com.capis.DataTypes.RespValues.RespValue;

public class PingCommand implements Command {
    private final String name = "PING";

    @Override
    public String getName() {
        return name;
    }

    @Override
    public RespValue execute(String[] args) {
        return switch (args.length) {
            case 1 -> new RespSimpleString("PONG");
            case 2 -> new RespBulkString(args[1]);
            default -> new RespErr("ERR", "PING command takes at most one argument.");
        };
    }
}
