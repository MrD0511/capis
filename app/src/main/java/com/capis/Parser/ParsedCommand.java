package com.capis.Parser;

public class ParsedCommand {
    private final String[] command;
    private final String resp;

    public ParsedCommand(String[] command, String resp) {
        this.command = command;
        this.resp = resp;
    }

    public String[] getCommand() {
        return command;
    }

    public String getResp() {
        return resp;
    }
}
