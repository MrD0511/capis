package com.capis.Commands;

import java.util.ArrayList;
import java.util.List;

import com.capis.DataTypes.RespValues.RespArray;
import com.capis.DataTypes.RespValues.RespErr;
import com.capis.DataTypes.RespValues.RespSimpleString;
import com.capis.DataTypes.RespValues.RespValue;
import com.capis.Network.ClientState;


public class CommandHandler {
    private final CommandRegistry commandRegistery;
    private final TransactionManager transactionManager = new TransactionManager();
    
    public CommandHandler(CommandRegistry commandRegistery) {
        this.commandRegistery = commandRegistery;
    }

    public RespValue execute(String[] args, ClientState clientState) {
        if (args.length == 0) {
            return new RespErr("ERR", "No command provided.\r\n");
        }   

        String commandName = args[0].toUpperCase();

        Command command = commandRegistery.get(commandName);

        switch (commandName) {
            case "MULTI" -> {
                if (transactionManager.isActive(clientState)) {
                    return new RespErr("ERR", "MULTI calls can not be nested");
                }
                
                transactionManager.begin(clientState);
                return new RespSimpleString("OK");
            }
            case "EXEC" -> {
                if (!transactionManager.isActive(clientState)) {
                    return new RespErr("ERR", "EXEC without MULTI");
                }
                
                return executeTransaction(clientState);
            }
            case "DISCARD" -> {
                if (!transactionManager.isActive(clientState)) {
                    return new RespErr("ERR", "DISCARD without MULTI");
                }
                transactionManager.end(clientState);
                return new RespSimpleString("OK");
            }
            default -> {
                if (transactionManager.isActive(clientState)) {
                    transactionManager.queue(clientState, args);
                    return new RespSimpleString("QUEUED");
                }
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

        List<String[]> queuedCommands = transactionManager.takeQueuedCommands(clientState);
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

        Command command = commandRegistery.get(commandName);

        if (command == null) {
            return new RespErr("ERR", "Unknown command '" + commandName + "'");
        }

        return command.execute(args);
    }

    public Command GetCommand(String commandName) {
        return commandRegistery.get(commandName);
    }
}
