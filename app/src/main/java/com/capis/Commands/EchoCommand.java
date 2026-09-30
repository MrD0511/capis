package com.capis.Commands;

import com.capis.DataTpes.RespValues.RespBulkString;
import com.capis.DataTpes.RespValues.RespErr;
import com.capis.DataTpes.RespValues.RespSimpleString;
import com.capis.DataTpes.RespValues.RespValue;

public class EchoCommand implements Command {
    String name = "ECHO";

    public String getName() {
        return name;
    }
    public RespValue execute(String[] args) {
        if (args.length == 1) {
            return new RespSimpleString("ECHO");
        } else if (args.length == 2) {
            return new RespBulkString(args[1]);
        } else {
            return new RespErr("ERR", "ECHO command takes at most one argument.\r\n");
        }
    }
}
