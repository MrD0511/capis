package com.capis;


public class App {
    public static void main(String[] args) throws Exception {

        AppConfig appConfig = ConfigLoader.load(args);

        Bootstrap bootstrap = new Bootstrap(appConfig);
        
        NioServer server = bootstrap.build();
        
        printBanner(appConfig.getPort(), appConfig.getEvictionPolicy().name(), appConfig.getCapacity(), appConfig.getPurgeLimit());

        try {
            server.start();
        } catch (Exception e) {
            System.err.println("Error starting server:");
            e.printStackTrace();
        }
    }

    private static void printBanner(int port, String evictionPolicy, int capacity, int purgeLimit) {
        System.out.println();
        System.out.println();
        System.out.println();


        System.out.println("   ██████╗ █████╗ ██████╗ ██╗███████╗\n" + //
                        "  ██╔════╝██╔══██╗██╔══██╗██║██╔════╝\n" + //
                        "  ██║     ███████║██████╔╝██║███████╗\n" + //
                        "  ██║     ██╔══██║██╔═══╝ ██║╚════██║\n" + //
                        "  ╚██████╗██║  ██║██║     ██║███████║\n" + //
                        "   ╚═════╝╚═╝  ╚═╝╚═╝     ╚═╝╚══════╝");
        System.out.println();
        System.out.println();

        System.out.println("CAPIS v0.1.0 - A Redis like In-Memory Key-Value Store");
        System.out.println("Listening on port " + port + "  ·  Ready to accept connections");
        System.out.println("Using " + evictionPolicy + " eviction policy with capacity " + capacity + " and purge limit " + purgeLimit);

        System.out.println();
        System.out.println();

    }
}
