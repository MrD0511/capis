package com.capis.AOF;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class AOF {

    private BufferedWriter writer;

    private String fileName;

    public AOF(String fileName) {
        this.fileName = fileName;
        try {
            openWriter(this.fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void open(String fileName) throws IOException {
        openWriter(fileName);
    }

    private void openWriter(String fileName) throws IOException {
        writer = new BufferedWriter(new FileWriter(fileName, true));
    }

    public void append(String data) throws IOException {
        writer.write(data);
        writer.flush();
    }

    public void close() throws IOException {
        if (writer != null) {
            writer.close();
            writer = null;
        }
    }

    public byte[] readAll() throws IOException {
        return Files.readAllBytes(Paths.get(fileName));
    }
}