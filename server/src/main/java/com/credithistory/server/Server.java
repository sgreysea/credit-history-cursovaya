
package com.credithistory.server;


import java.util.logging.Logger;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;

public class Server {

    private static final Logger logger = LoggerUtil.getLogger(Server.class);

    private static final int PORT = 8080;

    private final ExecutorService pool = Executors.newFixedThreadPool(10);

    public void start() {
        // проверку просрочек
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            PaymentDAO paymentDAO = new PaymentDAO();
            int overdueCount = paymentDAO.markOverduePayments();
            if (overdueCount > 0) {
                logger.info("Автоматически отмечено просроченных платежей: " + overdueCount);
            }
        }, 0, 1, java.util.concurrent.TimeUnit.HOURS);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            logger.info("Сервер запущен на порту "+ PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                logger.info("Подключился клиент: "+ clientSocket.getInetAddress());

                pool.execute(new ClientHandler(clientSocket));
            }

        } catch (IOException e) {
            logger.severe("Ошибка сервера"+ e);
        }
    }
}