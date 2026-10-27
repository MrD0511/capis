package com.capis;

import java.nio.ByteBuffer;

import com.capis.Commands.CommandHandler;
import com.capis.DataTpes.RespValues.RespErr;
import com.capis.DataTpes.RespValues.RespValue;
import com.capis.Network.ClientState;
import com.capis.Parser.ParsedCommand;
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
            ParsedCommand parsedValue = parser.decode(buffer);

            if(parsedValue == null){
                break;
            }

            if(parsedValue.getCommand().length == 0){
                output.append(parser.encode(new RespErr("ERR", "empty command")));
            }

            RespValue result = commandHandler.execute(parsedValue.getCommand(), clientState);

            System.out.println("parsed resp: " + parsedValue.getResp());

            output.append(parser.encode(result));
        }

        return output.toString();
    }
}
