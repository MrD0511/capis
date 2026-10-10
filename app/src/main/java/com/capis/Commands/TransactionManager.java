package com.capis.Commands;

import java.util.ArrayList;
import java.util.List;

import com.capis.Network.ClientState;

public class TransactionManager {
    public void begin(ClientState clientState) {
        clientState.beginTransaction();
    }

    public void queue(ClientState clientState, String[] command) {
        clientState.queueCommand(command);
    }

    public List<String[]> takeQueuedCommands(ClientState clientState) {
        return new ArrayList<>(clientState.getQueue());
    }

    public void end(ClientState clientState) {
        clientState.endTransaction();
    }

    public boolean isActive(ClientState clientState) {
        return clientState.isInTransaction();
    }
}
