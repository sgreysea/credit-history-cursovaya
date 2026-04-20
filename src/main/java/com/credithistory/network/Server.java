package com.credithistory.network;

import com.credithistory.util.LoggerUtil;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    private static final Logger logger = LoggerUtil.getLogger(Server.class);

    private static final int PORT = 8080;

    private final ExecutorService pool = Executors.newFixedThreadPool(10);

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            logger.info("Сервер запущен на порту {}", PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                logger.info("Подключился клиент: {}", clientSocket.getInetAddress());

                pool.execute(new ClientHandler(clientSocket));
            }

        } catch (IOException e) {
            logger.error("Ошибка сервера", e);
        }
    }
}