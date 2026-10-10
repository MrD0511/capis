package com.capis;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import com.capis.Network.ClientState;

public class NioServer {

    int port = 6379; // Default Redis port
    private CapisCore capisCore;
    private Selector selector;

    public NioServer(int port, CapisCore capisCore) throws IOException {
        this.port = port;
        this.capisCore = capisCore;
        this.selector = Selector.open();
    }

    public void start() throws Exception {
        ServerSocketChannel server = ServerSocketChannel.open();
        server.bind(new InetSocketAddress(this.port));
        server.configureBlocking(false);
        server.register(selector, SelectionKey.OP_ACCEPT);

        while (true) {
            selector.select();
            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();

            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove();

                // Skip keys that are no longer valid
                if (!key.isValid()) {
                    continue;
                }

                try {
                    if (key.isAcceptable()) {
                        acceptClient(key, server);
                    } else if (key.isReadable()) {
                        read(key);
                    }
                } catch (Exception e) {
                    // Catch-all block for the loop to keep the server alive 
                    // if an unhandled edge-case slips out
                    System.err.println("Error processing key: " + e.getMessage());
                    closeConnection(key);
                }
            }
        }
    }

    private void acceptClient(SelectionKey key, ServerSocketChannel server) throws IOException {
        SocketChannel client = server.accept();
        if (client == null) return; // Guard against spurious wakeups
        
        client.configureBlocking(false);
        ClientState clientState = new ClientState();
        client.register(selector, SelectionKey.OP_READ, clientState);

        System.out.println("Client connected: " + client.getRemoteAddress());
    }

    private void read(SelectionKey key) {
        SocketChannel client = (SocketChannel) key.channel();
        ClientState clientState = (ClientState) key.attachment();
        ByteBuffer buffer = clientState.getInputBuffer();

        try {
            int bytesRead = client.read(buffer);

            // Client initiated graceful close (-1 means EOF)
            if (bytesRead == -1) {
                System.out.println("Client disconnected cleanly.");
                closeConnection(key);
                return;
            }

            buffer.flip();
            String response = this.capisCore.handle(buffer, clientState);
            
            // Clear the buffer immediately after handling so it's clean for the next cycle
            buffer.clear();

            sendMessage(client, response);

        } catch (IOException e) {
            // Catches standard connection resets (e.g. client dropped forcefully)
            System.out.println("Client disconnected forcefully (IOException).");
            closeConnection(key);
        } catch (Exception e) {
            // Engine processing errors or internal bugs
            System.err.println("Internal handling error: " + e.getMessage());
            e.printStackTrace();
            
            // Safely attempt to send an error message, catch if the write itself fails
            try {
                sendMessage(client, "-ERR Internal server error\r\n");
            } catch (IOException ioException) {
                closeConnection(key);
            }
        }
    }

    static void sendMessage(SocketChannel client, String message) throws IOException {
        if (client.isOpen() && client.isConnected()) {
            ByteBuffer response = StandardCharsets.UTF_8.encode(message);
            client.write(response);
        } else {
            throw new ClosedChannelException();
        }
    }

    // Helper to safely tear down and cancel the selection key
    private void closeConnection(SelectionKey key) {
        if (key != null) {
            key.cancel();
            try {
                key.channel().close();
            } catch (IOException e) {
                // Ignore double close exceptions
            }
        }
    }
}
