<<<<<<< HEAD
package com.credithistory.network;

import com.credithistory.util.LoggerUtil;
import org.apache.logging.log4j.Logger;
import com.credithistory.model.User;
import com.credithistory.service.UserService;
import com.credithistory.service.CreditService;
import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private static final Logger logger = LoggerUtil.getLogger(ClientHandler.class);

    private final Socket socket;
    private final UserService userService;
    private final CreditService creditService;
    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.userService = new UserService();
        this.creditService = new CreditService();
    }

    @Override
    public void run() {
        try (
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(
                        socket.getOutputStream(), true)
        ) {

            String request;

            while ((request = in.readLine()) != null) {
                logger.info("Получен запрос: {}", request);

                String response = processRequest(request);

                out.println(response);
            }

        } catch (IOException e) {
            logger.error("Ошибка клиента", e);
        }
    }
    private String handleRegister(String[] parts) {
        if (parts.length < 3) {
            return "error: use register login password";
        }

        String login = parts[1];
        String password = parts[2];

        return userService.register(login, password);
    }
    private String handleLogin(String[] parts) {
        if (parts.length < 3) {
            return "error: use login login password";
        }

        String login = parts[1];
        String password = parts[2];

        if (login.equals("root") && password.equals("root123")) {
            return "super admin logged in";
        }

        return userService.login(login, password);
    }
    private String handleCreateCredit(String[] parts) {
        if (parts.length < 5) {
            return "error: use create_credit login amount rate term";
        }

        String login = parts[1];
        double amount = Double.parseDouble(parts[2]);
        double rate = Double.parseDouble(parts[3]);
        int term = Integer.parseInt(parts[4]);

        User user = userService.getUser(login);

        if (user == null) {
            return "error: user not found";
        }

        return creditService.createCredit(user, amount, rate, term);
    }
    private String handleMyScore(String[] parts) {
        if (parts.length < 2) {
            return "error: use my_score login";
        }

        String login = parts[1];
        User user = userService.getUser(login);

        if (user == null) {
            return "error: user not found";
        }

        int score = creditService.getUserCreditScore(user);
        return "Your credit score: " + score;
    }
    private String handleMyCredits(String[] parts) {
        if (parts.length < 2) {
            return "error: use my_credits login";
        }

        String login = parts[1];
        User user = userService.getUser(login);

        if (user == null) {
            return "error: user not found";
        }

        var credits = creditService.getUserCredits(user);

        return "credits count: " + credits.size();
    }
    private String processRequest(String request) {

        String[] parts = request.split(" ");

        String command = parts[0];

        switch (command.toLowerCase()) {

            case "ping":
                return "pong";
            case "register":
                return handleRegister(parts);
            case "login":
                return handleLogin(parts);
            case "create_credit":
                return handleCreateCredit(parts);
            case "my_score":
                return handleMyScore(parts);
            case "my_credits":
                return handleMyCredits(parts);
            default:
                return "unknown command";
        }
    }
=======
package com.credithistory.network;

import com.credithistory.util.LoggerUtil;
import org.apache.logging.log4j.Logger;
import com.credithistory.model.User;
import com.credithistory.service.UserService;
import com.credithistory.service.CreditService;
import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private static final Logger logger = LoggerUtil.getLogger(ClientHandler.class);

    private final Socket socket;
    private final UserService userService;
    private final CreditService creditService;
    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.userService = new UserService();
        this.creditService = new CreditService();
    }

    @Override
    public void run() {
        try (
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(
                        socket.getOutputStream(), true)
        ) {

            String request;

            while ((request = in.readLine()) != null) {
                logger.info("Получен запрос: {}", request);

                String response = processRequest(request);

                out.println(response);
            }

        } catch (IOException e) {
            logger.error("Ошибка клиента", e);
        }
    }
    private String handleRegister(String[] parts) {
        if (parts.length < 3) {
            return "error: use register login password";
        }

        String login = parts[1];
        String password = parts[2];

        return userService.register(login, password);
    }
    private String handleLogin(String[] parts) {
        if (parts.length < 3) {
            return "error: use login login password";
        }

        String login = parts[1];
        String password = parts[2];

        if (login.equals("root") && password.equals("root123")) {
            return "super admin logged in";
        }

        return userService.login(login, password);
    }
    private String handleCreateCredit(String[] parts) {
        if (parts.length < 5) {
            return "error: use create_credit login amount rate term";
        }

        String login = parts[1];
        double amount = Double.parseDouble(parts[2]);
        double rate = Double.parseDouble(parts[3]);
        int term = Integer.parseInt(parts[4]);

        User user = userService.getUser(login);

        if (user == null) {
            return "error: user not found";
        }

        return creditService.createCredit(user, amount, rate, term);
    }
    private String handleMyScore(String[] parts) {
        if (parts.length < 2) {
            return "error: use my_score login";
        }

        String login = parts[1];
        User user = userService.getUser(login);

        if (user == null) {
            return "error: user not found";
        }

        int score = creditService.getUserCreditScore(user);
        return "Your credit score: " + score;
    }
    private String handleMyCredits(String[] parts) {
        if (parts.length < 2) {
            return "error: use my_credits login";
        }

        String login = parts[1];
        User user = userService.getUser(login);

        if (user == null) {
            return "error: user not found";
        }

        var credits = creditService.getUserCredits(user);

        return "credits count: " + credits.size();
    }
    private String processRequest(String request) {

        String[] parts = request.split(" ");

        String command = parts[0];

        switch (command.toLowerCase()) {

            case "ping":
                return "pong";
            case "register":
                return handleRegister(parts);
            case "login":
                return handleLogin(parts);
            case "create_credit":
                return handleCreateCredit(parts);
            case "my_score":
                return handleMyScore(parts);
            case "my_credits":
                return handleMyCredits(parts);
            default:
                return "unknown command";
        }
    }
>>>>>>> 9a25b7675c45b2149c90b056a1d7d77d419d7ecd
}