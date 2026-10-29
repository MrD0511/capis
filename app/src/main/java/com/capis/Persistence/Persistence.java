package com.capis.Persistence;

import java.io.IOException;

import com.capis.AOF.AOF;
import com.capis.Commands.Command;
import com.capis.DataTypes.RespValues.RespValue;
import com.capis.Parser.Parser;

public class Persistence {
    private final AOF aof = new AOF();

    public void handle(Command command, Parser parser, String[] args) {
        if(!command.changesState()){
            return;
        }
        
        RespValue resp = command.persistentCommand(args);

        String respString = parser.encode(resp);

        try {
            aof.append(respString);
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println(respString);
    }
}
