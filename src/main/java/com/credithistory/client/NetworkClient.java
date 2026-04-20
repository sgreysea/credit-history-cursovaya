package com.credithistory.client;

import java.io.*;
import java.net.Socket;

public class NetworkClient {
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;

    public boolean connect(String host, int port) {
        try {
            socket = new Socket(host, port);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public String sendCommand(String command) {
        try {
            writer.println(command);
            return reader.readLine();
        } catch (IOException e) {
            return "ERROR: " + e.getMessage();
        }
    }

    public void disconnect() {
        try {
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}