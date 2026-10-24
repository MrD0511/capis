package com.capis;

import com.capis.Commands.CommandRegistry;
import com.capis.Commands.AppendCommand;
import com.capis.Commands.DBSIZECommand;
import com.capis.Commands.DECRCommand;
import com.capis.Commands.DELCommand;
import com.capis.Commands.EXPIRECommand;
import com.capis.Commands.EchoCommand;
import com.capis.Commands.EXISTSCommand;
import com.capis.Commands.FLUSHALLCommand;
import com.capis.Commands.GETDELCommand;
import com.capis.Commands.GetCommand;
import com.capis.Commands.HELLOCommand;
import com.capis.Commands.HGETALLCommand;
import com.capis.Commands.HSETCommand;
import com.capis.Commands.INCRBYFLOATCommand;
import com.capis.Commands.INCRCommand;
import com.capis.Commands.LIndexCommand;
import com.capis.Commands.LLENCommand;
import com.capis.Commands.LPOPCommand;
import com.capis.Commands.LPUSHCommand;
import com.capis.Commands.LRangeCommand;
import com.capis.Commands.MGetCommand;
import com.capis.Commands.MSetCommand;
import com.capis.Commands.PERSISTCommand;
import com.capis.Commands.PingCommand;
import com.capis.Commands.RPOPCommand;
import com.capis.Commands.RPushCommand;
import com.capis.Commands.SADDCommand;
import com.capis.Commands.SCARDCommand;
import com.capis.Commands.SetCommand;
import com.capis.Commands.SETEXCommand;
import com.capis.Commands.SISMEMBERCommand;
import com.capis.Commands.SMEMBERSCommand;
import com.capis.Commands.SREMCommand;
import com.capis.Commands.STRLENCommand;
import com.capis.Commands.TTLCommand;
import com.capis.Commands.TypeCommand;
import com.capis.Commands.ZADDCommand;
import com.capis.Commands.ZRANGECommand;
import com.capis.Commands.CommandHandler;
import com.capis.DataTpes.Core.Value;
import com.capis.Network.ClientStateFactory;
import com.capis.Network.NioServer;
import com.capis.Parser.Parser;
import com.capis.entities.Cache;
import com.capis.entities.CacheFactory;
import com.capis.entities.KeyValueStore;

public class Bootstrap {


    
    private AppConfig appConfig;

    public Bootstrap(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    public NioServer build(){
        Cache<String, Value<?>> cache = new CacheFactory().createCache(appConfig.getCapacity(), appConfig.getEvictionPolicy(), appConfig.getPurgeLimit()); 

        KeyValueStore keyValueStore = new KeyValueStore(cache);

        MaintainanceService maintainanceService = new MaintainanceService(keyValueStore);

        Parser parser = new Parser();

        CommandRegistry commandRegistry = new CommandRegistry();

        addCommandsToRegistry(commandRegistry, keyValueStore);

        CommandHandler commandHandler = new CommandHandler(commandRegistry);

        CapisCore capisCore = new CapisCore(parser, commandHandler);

        ClientStateFactory clientStateFactory = new ClientStateFactory(appConfig.getBufferSize());

        try {
            return new NioServer(this.appConfig.getPort(), 
                                capisCore, 
                                maintainanceService, 
                                clientStateFactory);
        } catch (Exception e) {
            System.err.println("Error starting server:");
            e.printStackTrace();
            return null;
        }
    }

    private void addCommandsToRegistry(CommandRegistry commandRegistry, KeyValueStore keyValueStore) {
        commandRegistry.register(new SetCommand(keyValueStore));
        commandRegistry.register(new GetCommand(keyValueStore));
        commandRegistry.register(new MSetCommand(keyValueStore));
        commandRegistry.register(new MGetCommand(keyValueStore));
        commandRegistry.register(new AppendCommand(keyValueStore));
        commandRegistry.register(new STRLENCommand(keyValueStore));
        commandRegistry.register(new GETDELCommand(keyValueStore));
        commandRegistry.register(new SETEXCommand(keyValueStore));
        commandRegistry.register(new DELCommand(keyValueStore));
        commandRegistry.register(new EXISTSCommand(keyValueStore));
        commandRegistry.register(new DBSIZECommand(keyValueStore));
        commandRegistry.register(new TypeCommand(keyValueStore));
        commandRegistry.register(new FLUSHALLCommand(keyValueStore));
        commandRegistry.register(new EXPIRECommand(keyValueStore));
        commandRegistry.register(new PERSISTCommand(keyValueStore));
        commandRegistry.register(new TTLCommand(keyValueStore));
        commandRegistry.register(new INCRCommand(keyValueStore));
        commandRegistry.register(new DECRCommand(keyValueStore));
        commandRegistry.register(new INCRBYFLOATCommand(keyValueStore));
        commandRegistry.register(new LPUSHCommand(keyValueStore));
        commandRegistry.register(new RPushCommand(keyValueStore));
        commandRegistry.register(new LLENCommand(keyValueStore));
        commandRegistry.register(new LIndexCommand(keyValueStore));
        commandRegistry.register(new LRangeCommand(keyValueStore));
        commandRegistry.register(new LPOPCommand(keyValueStore));
        commandRegistry.register(new RPOPCommand(keyValueStore));
        commandRegistry.register(new SADDCommand(keyValueStore));
        commandRegistry.register(new SREMCommand(keyValueStore));
        commandRegistry.register(new SCARDCommand(keyValueStore));
        commandRegistry.register(new SISMEMBERCommand(keyValueStore));
        commandRegistry.register(new SMEMBERSCommand(keyValueStore));
        commandRegistry.register(new HSETCommand(keyValueStore));
        commandRegistry.register(new HGETALLCommand(keyValueStore));
        commandRegistry.register(new ZADDCommand(keyValueStore));
        commandRegistry.register(new ZRANGECommand(keyValueStore));
        commandRegistry.register(new PingCommand());
        commandRegistry.register(new EchoCommand());
        commandRegistry.register(new HELLOCommand());
    }


}
