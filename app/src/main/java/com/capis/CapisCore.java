package com.capis;

import java.nio.ByteBuffer;

import com.capis.Commands.Command;
import com.capis.Commands.CommandHandler;
import com.capis.DataTypes.RespValues.RespErr;
import com.capis.DataTypes.RespValues.RespValue;
import com.capis.Network.ClientState;
import com.capis.Parser.Parser;
import com.capis.Persistence.Persistence;

public class CapisCore  {
    private final Parser parser;
    private final CommandHandler commandHandler;
    private final Persistence persistence;

    public CapisCore(Parser parser, CommandHandler commandHandler, Persistence persistence) {
        this.parser = parser;
        this.commandHandler = commandHandler;
        this.persistence = persistence;
    }

    public String handle(ByteBuffer buffer, ClientState clientState) throws Exception {
        StringBuilder output = new StringBuilder();

        while(buffer.hasRemaining()){
            String[] parsedCommand = parser.decode(buffer);

            if(parsedCommand == null){
                break;
            }

            if(parsedCommand.length == 0){
                output.append(parser.encode(new RespErr("ERR", "empty command")));
                continue;
            }

            RespValue result = commandHandler.execute(parsedCommand, clientState);

            Command command = commandHandler.GetCommand(parsedCommand[0].toUpperCase());

            if(command != null && !(result instanceof RespErr)){
                persistence.handle(command , parser, parsedCommand);
            }

            output.append(parser.encode(result));
        }

        return output.toString();
    }
}
