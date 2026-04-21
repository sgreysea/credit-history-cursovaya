package com.credithistory;

import com.credithistory.network.Server;

public class Main {
    public static void main(String[] args) {
        System.out.println("Запуск сервера системы учета кредитных историй...");
        Server server = new Server();
        server.start();
    }
}