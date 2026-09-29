package com.capis;


// import com.capis.entities.LRUCache;
import com.capis.entities.Parser;
import java.nio.charset.StandardCharsets;
import com.capis.entities.CommandHandler;
import com.capis.entities.KeyValueStore;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    public static void main(String[] args) throws Exception {

        int port = 6379; // Default Redis port


        KeyValueStore keyValueStore = new KeyValueStore(100); // Capacity of 100 entries
        Parser parser = new Parser();
        CommandHandler commandHandler = new CommandHandler();

        ServerSocket serverSocket = new ServerSocket(port);

        System.out.println();
        System.out.println();
        System.out.println();


        System.out.println("   ██████╗ █████╗ ██████╗ ██╗███████╗\n" + //
                        "  ██╔════╝██╔══██╗██╔══██╗██║██╔════╝\n" + //
                        "  ██║     ███████║██████╔╝██║███████╗\n" + //
                        "  ██║     ██╔══██║██╔═══╝ ██║╚════██║\n" + //
                        "  ╚██████╗██║  ██║██║     ██║███████║\n" + //
                        "  ╚═════╝╚═╝  ╚═╝╚═╝     ╚═╝╚══════╝");
        System.out.println();
        System.out.println();

        System.out.println("CAPIS v0.1.0 - A Simple In-Memory Key-Value Store");
        System.out.println("Listening on port 6379  ·  Ready to accept connections");

        System.out.println();
        System.out.println();

        while(true){
            Socket socket = serverSocket.accept();
            System.out.println("Accepted connection from " + socket.getInetAddress());

            InputStream input = socket.getInputStream();
            OutputStream output = socket.getOutputStream();

            while(true){
                String[] command = parser.parse(input);
                if (command == null) {
                    break;
                }
                if(command.length == 0){
                    output.write("-ERR Empty command\r\n".getBytes(StandardCharsets.UTF_8));
                    output.flush();
                    continue;
                }

                String response = commandHandler.execute(command, keyValueStore);

                output.write(response.getBytes());
                output.flush();
            }

            socket.close();
        }

    }
}