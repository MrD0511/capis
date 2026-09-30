package com.capis.Commands;

import java.util.HashMap;
import java.util.Map;

import com.capis.DataTpes.RespValues.RespErr;
import com.capis.DataTpes.RespValues.RespSimpleString;
import com.capis.DataTpes.RespValues.RespValue;


public class CommandHandler {
    Map<String, Command> commandMap;

    public CommandHandler() {
        this.commandMap = new HashMap<>();
    }

    public void registerCommand(Command command) {
        commandMap.put(command.getName(), command);
    }

    public RespValue execute(String[] args){
        if (args.length == 0) {
            return new RespErr("ERR", "No command provided.\r\n");
        }   

        String commandName = args[0].toUpperCase();
        Command command = commandMap.get(commandName);

        if (command == null) {
            return new RespSimpleString("OK");
        }

        return command.execute(args);
    }
}
