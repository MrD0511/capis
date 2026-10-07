package com.capis;

public class App {
    public static void main(String[] args) throws Exception {
        CapisCore capisCore = new CapisCore(1000);
        NioServer server = new NioServer(6379, capisCore);
        
        printBanner();

        try {
            server.start();
        } catch (Exception e) {
            System.err.println("Error starting server:");
            e.printStackTrace();
        }
    }

    private static void printBanner(){
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

        System.out.println("CAPIS v0.1.0 - A Simple In-Memory Key-Value Store");
        System.out.println("Listening on port 6379  ·  Ready to accept connections");

        System.out.println();
        System.out.println();

    }
}
