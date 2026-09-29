package com.capis.entities;

public class CommandHandler {
    public String execute(String[] command, KeyValueStore cache) {
        if(command.length == 0){
            return "-ERR Empty command\r\n";
        }

        String cmd = command[0].toUpperCase();
        System.out.println("Executing command: " + cmd);
        switch(cmd){
            case "PING":
                return "+PONG\r\n";
            case "ECHO":
                if(command.length < 2){
                    return "-ERR ECHO command requires an argument\r\n";
                }

                return "+" + command[1] + "\r\n";
            case "GET":
                if(command.length < 2){
                    return "-ERR GET command requires a key\r\n";
                }

                String value = cache.get(command[1]);
                if(value == null){
                    return "$-1\r\n"; // RESP Null Bulk String
                } else {
                    return "$" + value.length() + "\r\n" + value + "\r\n";
                }
            case "SET":
                if(command.length < 3){
                    return "-ERR SET command requires a key and a value\r\n";
                }

                cache.put(command[1], command[2]);
                return "+OK\r\n";
            default:
                return "-ERR Unknown command\r\n";
        }
    }
}
