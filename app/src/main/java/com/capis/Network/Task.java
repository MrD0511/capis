package com.capis.Network;

import java.nio.channels.SocketChannel;

public class Task {
    private final SocketChannel client;
    private final ClientState clientState;

    public Task(SocketChannel client, ClientState clientState) {
        this.client = client;
        this.clientState = clientState;
    }

    public SocketChannel getClient() {
        return client;
    }

    public ClientState getClientState() {
        return clientState;
    }

}
