package com.capis.Commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.capis.DataTpes.RespValues.RespArray;
import com.capis.DataTpes.RespValues.RespErr;
import com.capis.DataTpes.RespValues.RespSimpleString;
import com.capis.DataTpes.RespValues.RespValue;
import com.capis.Network.ClientState;


public class CommandHandler {
    Map<String, Command> commandMap;
    TransactionManager transactionManager = new TransactionManager();

    public CommandHandler() {
        this.commandMap = new HashMap<>();
    }

    public void registerCommand(Command command) {
        commandMap.put(command.getName(), command);
    }

    public RespValue execute(String[] args, ClientState clientState) {
        if (args.length == 0) {
            return new RespErr("ERR", "No command provided.\r\n");
        }   

        String commandName = args[0].toUpperCase();

        Command command = commandMap.get(commandName);

        switch (commandName) {
            case "MULTI":
                if (transactionManager.isActive(clientState)) {
                    return new RespErr("ERR", "MULTI calls can not be nested");
                }
                
                transactionManager.begin(clientState);
                return new RespSimpleString("OK");
            case "EXEC":
                if (!transactionManager.isActive(clientState)) {
                    return new RespErr("ERR", "EXEC without MULTI");
                }
                
                return executeTransaction(clientState);
            case "DISCARD":
                if (!transactionManager.isActive(clientState)) {
                    return new RespErr("ERR", "DISCARD without MULTI");
                }
                transactionManager.end(clientState);
                return new RespSimpleString("OK");
            default:
                if (transactionManager.isActive(clientState)) {
                    transactionManager.queue(clientState, args);
                    return new RespSimpleString("QUEUED");
                }
        }

        if (command == null) {
            return new RespErr("ERR", "Unknown command '" + commandName + "'");
        }

        return command.execute(args);
    }

    public RespValue executeTransaction(ClientState clientState) {
        if (!transactionManager.isActive(clientState)) {
            return new RespErr("ERR", "EXEC without MULTI");
        }
        var queuedCommands = transactionManager.takeQueuedCommands(clientState);
        transactionManager.end(clientState);

        List<RespValue> results = new ArrayList<>();
        
        for (int i = 0; i < queuedCommands.size(); i++) {
            results.add(executeImmediately(queuedCommands.get(i)));
        }
        
        return new RespArray(results);
    }

    public RespValue executeImmediately(String[] args) {
        if (args.length == 0) {
            return new RespErr("ERR", "No command provided.\r\n");
        }   

        String commandName = args[0].toUpperCase();

        Command command = commandMap.get(commandName);

        if (command == null) {
            return new RespErr("ERR", "Unknown command '" + commandName + "'");
        }

        return command.execute(args);
    }
}
