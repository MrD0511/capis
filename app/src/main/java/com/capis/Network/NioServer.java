package com.capis.Network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;

import com.capis.CapisCore;
import com.capis.MaintainanceService;

public class NioServer {

    int port = 6379; // Default Redis port
    final private CapisCore capisCore;
    final private Selector selector;
    final private MaintainanceService maintainanceService;
    final private Queue<Task> pendingTasks = new ArrayDeque<>();
    final private int TASKS_EXECUTION_LIMIT = 100; // Arbitrary limit to prevent memory exhaustion

    public NioServer(int port, CapisCore capisCore, MaintainanceService maintainanceService) throws IOException {
        this.port = port;
        this.capisCore = capisCore;
        this.selector = Selector.open();
        this.maintainanceService = maintainanceService;
    }

    public void start() throws Exception {
        ServerSocketChannel server = ServerSocketChannel.open();
        server.bind(new InetSocketAddress(this.port));
        server.configureBlocking(false);
        server.register(selector, SelectionKey.OP_ACCEPT);

        while (true) {
            this.maintainanceService.maintain();

            selector.select(10);

            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();

            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove();

                if (!key.isValid()) continue;

                if (key.isAcceptable()) {
                    acceptClient(server);
                } else if (key.isReadable()) {
                    read(key);
                }
            }

            executePendingTasks();
        }
    }

    private void acceptClient(ServerSocketChannel server) throws IOException {
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

            Task task = new Task(client, clientState);
            pendingTasks.add(task);
            
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

    private void executePendingTasks(){
        int tasksExecuted = 0;
        while (tasksExecuted < TASKS_EXECUTION_LIMIT && !pendingTasks.isEmpty()) {
            Task task = pendingTasks.poll();
            if (task != null) {
                executePendingTask(task);
                tasksExecuted++;
            }
        }
    }

    private void executePendingTask(Task task) {
        SocketChannel client = task.getClient();
        ClientState clientState = task.getClientState();
        ByteBuffer buffer = clientState.getInputBuffer();

        try {

            buffer.flip(); 
            // Process the command using CapisCore
            String response = capisCore.handle(buffer, clientState);

            buffer.compact();

            sendMessage(client, response);
        } catch (IOException e) {
            System.err.println("Failed to send response to client: " + e.getMessage());
            closeConnection(client.keyFor(selector));
        }catch (Exception e) {
            System.err.println("Error processing client command: " + e.getMessage());
            e.printStackTrace();
            try {
                sendMessage(client, "-ERR Internal server error\r\n");
            } catch (IOException ioException) {
                closeConnection(client.keyFor(selector));
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
