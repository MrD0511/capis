package com.capis.Commands;

import java.util.HashMap;
import java.util.Map;

public class CommandRegistry {
    Map<String, Command> commandMap;

    public CommandRegistry() {
        this.commandMap = new HashMap<>();
    }

    public void register(Command command) {
        commandMap.put(command.getName(), command);
    }

    public Command get(String name) {
        return commandMap.get(name.toUpperCase());
    }
}
