package com.capis;

import com.capis.Config.AppConfig;
import com.capis.Config.ConfigLoader;
import com.capis.Network.NioServer;

public class App {
    public static void main(String[] args) throws Exception {

        AppConfig appConfig = new ConfigLoader().load(args);

        Bootstrap bootstrap = new Bootstrap(appConfig);
        
        NioServer server = bootstrap.build();
        
        Banner.print(appConfig.getPort(), appConfig.getEvictionPolicy().name(), appConfig.getCapacity(), appConfig.getPurgeLimit());

        try {
            server.start();
        } catch (Exception e) {
            System.err.println("Error starting server:");
            e.printStackTrace();
        }
    }
}
