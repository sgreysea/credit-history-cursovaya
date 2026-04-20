<<<<<<< HEAD
package com.credithistory.client;

import javafx.fxml.FXML;
import javafx.scene.control.*;

public class LoginController {

    @FXML private TextField loginField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;

    private NetworkClient networkClient;

    @FXML
    private void handleLogin() {
        String login = loginField.getText().trim();
        String password = passwordField.getText().trim();

        if (login.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Введите логин и пароль");
            return;
        }

        statusLabel.setText("Подключение...");

        new Thread(() -> {
            networkClient = new NetworkClient();

            if (!networkClient.connect("localhost", 8080)) {
                updateStatus("Сервер не запущен!");
                return;
            }

            updateStatus("Отправка...");
            String response = networkClient.sendCommand("login " + login + " " + password);
            updateStatus("Ответ: " + response);
        }).start();
    }

    @FXML
    private void handleRegister() {
        statusLabel.setText("Регистрация (будет позже)");
    }

    private void updateStatus(String message) {
        javafx.application.Platform.runLater(() ->
                statusLabel.setText(message)
        );
    }
=======
package com.credithistory.client;

import javafx.fxml.FXML;
import javafx.scene.control.*;

public class LoginController {

    @FXML private TextField loginField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;

    private NetworkClient networkClient;

    @FXML
    private void handleLogin() {
        String login = loginField.getText().trim();
        String password = passwordField.getText().trim();

        if (login.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Введите логин и пароль");
            return;
        }

        statusLabel.setText("Подключение...");

        new Thread(() -> {
            networkClient = new NetworkClient();

            if (!networkClient.connect("localhost", 8080)) {
                updateStatus("Сервер не запущен!");
                return;
            }

            updateStatus("Отправка...");
            String response = networkClient.sendCommand("login " + login + " " + password);
            updateStatus("Ответ: " + response);
        }).start();
    }

    @FXML
    private void handleRegister() {
        statusLabel.setText("Регистрация (будет позже)");
    }

    private void updateStatus(String message) {
        javafx.application.Platform.runLater(() ->
                statusLabel.setText(message)
        );
    }
>>>>>>> 9a25b7675c45b2149c90b056a1d7d77d419d7ecd
}