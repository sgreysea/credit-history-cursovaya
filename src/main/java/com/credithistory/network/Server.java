<<<<<<< HEAD
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
=======
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
>>>>>>> 9a25b7675c45b2149c90b056a1d7d77d419d7ecd
}