package com.capis.Network;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class ClientState {
    private boolean inTransaction = false;
    private List<String[]> queue = new ArrayList<>();
    private ByteBuffer inputBuffer = ByteBuffer.allocate(8192); // 8KB buffer


    public void discardTransaction() {
        queue.clear();
        inTransaction = false;
    }

    public boolean isInTransaction() {
        return inTransaction;
    }

    public void beginTransaction() {
        this.inTransaction = true;
    }

    public void endTransaction() {
        this.inTransaction = false;
        this.queue.clear();
    }

    public void queueCommand(String[] command) {
        this.queue.add(command);
    }

    public List<String[]> getQueue() {
        return queue;
    }

    public ByteBuffer getInputBuffer() {
        return inputBuffer;
    }
}
