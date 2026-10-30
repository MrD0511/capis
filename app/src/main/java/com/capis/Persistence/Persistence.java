package com.capis.Persistence;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.capis.Commands.Command;
import com.capis.Commands.CommandHandler;
import com.capis.DataTypes.RespValues.RespErr;
import com.capis.DataTypes.RespValues.RespValue;
import com.capis.FileSystem.FileSystem;
import com.capis.Network.ClientState;
import com.capis.Parser.Parser;

public class Persistence {
    private final FileSystem fileSystem;

    public Persistence(FileSystem fileSystem) {
        this.fileSystem = fileSystem;
    }

    public void handle(Command command, Parser parser, String[] args) {
        if(!command.changesState()){
            return;
        }
        
        RespValue resp = command.persistentCommand(args);

        String respString = parser.encode(resp);

        try {
            fileSystem.append(respString);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void replay(
            Parser parser,
            CommandHandler commandHandler,
            ClientState replayClientState
    ) throws IOException {

        byte[] data = fileSystem.readAll();

        System.out.println(data.length + " bytes read from AOF file.");

        ByteBuffer buffer = ByteBuffer.wrap(data);


        while (buffer.hasRemaining()) {
            String[] args = parser.decode(buffer);

            if(args == null) {
                System.err.println("Error during AOF replay: Failed to decode command.");
                break;
            }

            if (args.length == 0) {
                continue;
            }

            // Execute the recorded command directly.
            // Do NOT call persistence.handle() here.
            RespValue result =
                    commandHandler.execute(args, replayClientState);

            if (result instanceof RespErr) {
                throw new IOException(
                        "AOF replay failed for command: " + args[0]
                );
            }
        }
    }

}
