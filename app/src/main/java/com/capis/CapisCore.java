package com.capis;

import java.nio.ByteBuffer;

import com.capis.Commands.CommandHandler;
import com.capis.DataTpes.RespValues.RespErr;
import com.capis.Network.ClientState;
import com.capis.Parser.Parser;

public class CapisCore  {
    private final Parser parser;
    private final CommandHandler commandHandler;

    public CapisCore(Parser parser, CommandHandler commandHandler) {
        this.parser = parser;
        this.commandHandler = commandHandler;
    }

    public String handle(ByteBuffer buffer, ClientState clientState) throws Exception {
        StringBuilder output = new StringBuilder();

        while(buffer.hasRemaining()){
            String[] command = parser.decode(buffer);
            if(command == null){
                break;
            }

            if(command.length == 0){
                output.append(parser.encode(new RespErr("ERR", "empty command")));
            }

            output.append(parser.encode(commandHandler.execute(command, clientState)));
        }

        return output.toString();
    }
}
