package com.credithistory.server;


public class Main {
    public static void main(String[] args) {
        System.out.println("запуск сервера системы учета кредитных историй...");
        ClientDAO.ensureSchema();
        Server server = new Server();
        server.start();
    }
}