package com.capis.Network;

public class ClientStateFactory {
    int bufferSize;

    public ClientStateFactory(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    public ClientState createClientState() {
        return new ClientState(bufferSize);
    }
}
