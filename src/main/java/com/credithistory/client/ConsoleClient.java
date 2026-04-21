
package com.credithistory.client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class ConsoleClient {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Введите логин:");
        String login = scanner.nextLine();
        System.out.println("Введите пароль:");
        String password = scanner.nextLine();

        try {
            Socket socket = new Socket("localhost", 8080);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println("login " + login + " " + password);
            String response = in.readLine();

            System.out.println("Ответ: " + response);
            socket.close();
        } catch (Exception e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }
}