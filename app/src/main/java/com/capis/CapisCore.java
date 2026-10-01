package com.capis;

import java.io.InputStream;

import com.capis.Commands.CommandHandler;
import com.capis.Commands.DECRCommand;
import com.capis.Commands.DELCommand;
import com.capis.Commands.EchoCommand;
import com.capis.Commands.GetCommand;
import com.capis.Commands.INCRCommand;
import com.capis.Commands.LIndexCommand;
import com.capis.Commands.LLENCommand;
import com.capis.Commands.LPOPCommand;
import com.capis.Commands.LPUSHCommand;
import com.capis.Commands.LRangeCommand;
import com.capis.Commands.MGetCommand;
import com.capis.Commands.MSetCommand;
import com.capis.Commands.PingCommand;
import com.capis.Commands.RPOPCommand;
import com.capis.Commands.RPushCommand;
import com.capis.Commands.SADDCommand;
import com.capis.Commands.SCARDCommand;
import com.capis.Commands.SISMEMBERCommand;
import com.capis.Commands.SMEMBERSCommand;
import com.capis.Commands.SREMCommand;
import com.capis.Commands.SetCommand;
import com.capis.DataTpes.RespValues.RespErr;
import com.capis.DataTpes.RespValues.RespValue;
import com.capis.Parser.Parser;
import com.capis.entities.KeyValueStore;

public class CapisCore {
    private KeyValueStore keyValueStore;
    private Parser parser;
    private CommandHandler commandHandler;

    public CapisCore(int capacity){
        this.keyValueStore = new KeyValueStore(capacity);
        this.parser = new Parser();
        this.commandHandler = new CommandHandler();

        this.commandHandler.registerCommand(new PingCommand());
        this.commandHandler.registerCommand(new EchoCommand());
        this.commandHandler.registerCommand(new GetCommand(this.keyValueStore));
        this.commandHandler.registerCommand(new SetCommand(this.keyValueStore));
        this.commandHandler.registerCommand(new MSetCommand(this.keyValueStore));
        this.commandHandler.registerCommand(new MGetCommand(this.keyValueStore));
        this.commandHandler.registerCommand(new LPUSHCommand(this.keyValueStore));
        this.commandHandler.registerCommand(new RPushCommand(this.keyValueStore));
        this.commandHandler.registerCommand(new LLENCommand(this.keyValueStore));
        this.commandHandler.registerCommand(new LIndexCommand(this.keyValueStore));
        this.commandHandler.registerCommand(new LRangeCommand(this.keyValueStore));
        this.commandHandler.registerCommand(new DELCommand(this.keyValueStore));
        this.commandHandler.registerCommand(new INCRCommand(this.keyValueStore));
        this.commandHandler.registerCommand(new DECRCommand(this.keyValueStore));
        this.commandHandler.registerCommand(new SADDCommand(this.keyValueStore));
        this.commandHandler.registerCommand(new SREMCommand(this.keyValueStore));
        this.commandHandler.registerCommand(new SCARDCommand(this.keyValueStore));
        this.commandHandler.registerCommand(new SISMEMBERCommand(this.keyValueStore));
        this.commandHandler.registerCommand(new SMEMBERSCommand(this.keyValueStore));
        this.commandHandler.registerCommand(new LPOPCommand(this.keyValueStore));
        this.commandHandler.registerCommand(new RPOPCommand(this.keyValueStore));
    }

    public String run(InputStream input) throws Exception{
        String[] command = parser.decode(input);

        if (command == null) {
            return null;
        }

        if (command.length == 0) {
            return parser.encode(new RespErr("ERR", "empty command"));
        }

        RespValue respVal = commandHandler.execute(command);

        String response = parser.encode(respVal);

        return response;
    }
}
