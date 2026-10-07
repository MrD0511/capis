package com.capis;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;



public class NioServer {

    int port = 6379; // Default Redis port
    private CapisCore capisCore;

    public NioServer(int port, CapisCore capisCore) {
        this.port = port;
        this.capisCore = capisCore;
    }

    public void start() throws Exception {
        
        Selector selector = Selector.open();

        // 2. Create server channel
        ServerSocketChannel server = ServerSocketChannel.open();

        // 3. Listen on port 6379
        server.bind(new InetSocketAddress(this.port));

        // 4. Make it non-blocking
        server.configureBlocking(false);

        // 5. Tell selector:
        //    "Tell me when a new client connects"
        server.register(selector, SelectionKey.OP_ACCEPT);

        // 6. Event loop
        while (true) {

            selector.select();

            Iterator<SelectionKey> iterator =
                    selector.selectedKeys().iterator();

            while (iterator.hasNext()) {

                SelectionKey key = iterator.next();

                // IMPORTANT:
                // Remove the key after processing it.
                iterator.remove();

                // New client connected
                if (key.isAcceptable()) {

                    ServerSocketChannel serverChannel =
                            (ServerSocketChannel) key.channel();

                    SocketChannel client =
                            serverChannel.accept();

                    client.configureBlocking(false);

                    // Tell selector:
                    // "Tell me when this client has data"
                    client.register(
                            selector,
                            SelectionKey.OP_READ
                    );

                    System.out.println(
                            "Client connected: " + client.getRemoteAddress()
                    );
                }

                // Client sent data
                else if (key.isReadable()) {
                    SocketChannel client = (SocketChannel) key.channel();
                    
                    try{

                        ByteBuffer buffer =
                                ByteBuffer.allocate(1024);

                        int bytesRead = client.read(buffer);

                        if (bytesRead == -1) {
                            client.close();
                            continue;
                        }

                        buffer.flip();

                        InputStream input = new InputStream() {
                            @Override
                            public int read() {
                                if (!buffer.hasRemaining()) {
                                    return -1;
                                }

                                return buffer.get() & 0xFF;
                            }
                        };
                        
                        String response = capisCore.run(input);

                        sendMessage(client, response);
                    }catch(Exception e){
                        e.printStackTrace();
                        sendMessage(client, "-ERR Internal server error\r\n");
                    }
                }
            }
        }
    }

    static void sendMessage(SocketChannel client, String message) throws IOException {

        ByteBuffer response =
                StandardCharsets.UTF_8.encode(message);

        client.write(response);
    }

}