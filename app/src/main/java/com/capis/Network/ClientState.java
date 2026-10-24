package com.capis.Network;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class ClientState {
    private boolean inTransaction = false;
    private List<String[]> queue = new ArrayList<>();
    private ByteBuffer inputBuffer;
    private ByteBuffer outputBuffer;

    private static final int MAX_INPUT = 64 * 1024 * 1024;

    public ClientState(int bufferSize) {
        this.inputBuffer = ByteBuffer.allocate(bufferSize); // Adjust size as needed
        this.outputBuffer = ByteBuffer.allocate(bufferSize); // Adjust size as needed
    }

    public ClientState() {
        this.inputBuffer = ByteBuffer.allocate(1024 * 16); // Default size
        this.outputBuffer = ByteBuffer.allocate(1024 * 16); // Default size
    }
    

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

    public ByteBuffer getOutputBuffer() {
        return outputBuffer;
    }

    public boolean growInputBuffer() {
        if (inputBuffer.capacity() >= MAX_INPUT) return false;
        ByteBuffer bigger = ByteBuffer.allocate(Math.min(inputBuffer.capacity() * 2, MAX_INPUT));
        inputBuffer.flip();
        bigger.put(inputBuffer);
        inputBuffer = bigger;
        return true;
    }

    public ByteBuffer ensureOutputCapacity(int extraBytes) {
        int needed = outputBuffer.position() + extraBytes;   // buffer is in write mode, so position = bytes pending
        if (needed <= outputBuffer.capacity()) return outputBuffer;

        int newCap = Math.max(outputBuffer.capacity() * 2, needed);
        ByteBuffer bigger = ByteBuffer.allocate(newCap);
        outputBuffer.flip();
        bigger.put(outputBuffer);          // copy unsent bytes, bigger stays in write mode
        outputBuffer = bigger;
        return outputBuffer;
    }
}
