package com.capis;


import java.nio.charset.StandardCharsets;

import com.capis.Parser.Parser;
import com.capis.Commands.CommandHandler;
import com.capis.Commands.EchoCommand;
import com.capis.Commands.GetCommand;
import com.capis.Commands.PingCommand;
import com.capis.Commands.SetCommand;
import com.capis.DataTpes.RespValues.RespValue;
import com.capis.entities.KeyValueStore;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    int port = 6379; // Default Redis port
    KeyValueStore keyValueStore;
    Parser parser;
    CommandHandler commandHandler;

    public Server(int port, int capacity){
        this.port = port;
        this.keyValueStore = new KeyValueStore(capacity);
        this.parser = new Parser();
        this.commandHandler = new CommandHandler();

        this.commandHandler.registerCommand(new PingCommand());
        this.commandHandler.registerCommand(new EchoCommand());
        this.commandHandler.registerCommand(new GetCommand(this.keyValueStore));
        this.commandHandler.registerCommand(new SetCommand(this.keyValueStore));
    } 

    public void start() throws Exception {
        ServerSocket serverSocket = new ServerSocket(this.port);

        printBanner();

        while (true) {
            Socket socket = serverSocket.accept();

            System.out.println(
                "Accepted connection from " + socket.getInetAddress()
            );

            try (
                socket;
                InputStream input = socket.getInputStream();
                OutputStream output = socket.getOutputStream()
            ) {
                while (true) {
                    String[] command = parser.decode(input);

                    if (command == null) {
                        System.out.println("Client disconnected");
                        break;
                    }

                    if (command.length == 0) {
                        output.write(
                            "-ERR Empty command\r\n"
                                .getBytes(StandardCharsets.UTF_8)
                        );
                        output.flush();
                        continue;
                    }

                    RespValue respVal = commandHandler.execute(command);

                    String response = parser.encode(respVal);

                    output.write(
                        response.getBytes(StandardCharsets.UTF_8)
                    );
                    output.flush();
                }

            } catch (java.net.SocketException e) {
                System.out.println("Client disconnected: " + e.getMessage());

            } catch (Exception e) {
                System.err.println("Error handling client:");
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Server server = new Server(6379, 100);

        server.start();
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