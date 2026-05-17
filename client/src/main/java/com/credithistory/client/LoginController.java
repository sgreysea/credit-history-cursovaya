package com.credithistory.client;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

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

        statusLabel.setText("Подключение к серверу...");

        new Thread(() -> {
            networkClient = new NetworkClient();

            if (!networkClient.connect("localhost", 8080)) {
                updateStatus("Ошибка: сервер не запущен");
                return;
            }

            updateStatus("Проверка учётных данных...");
            String response = networkClient.sendCommand("login " + login + " " + password);
            if (response == null) {
                updateStatus("Ошибка: нет ответа от сервера");
                networkClient.close();
                return;
            }

            if (response.startsWith("OK:")) {
                String[] parts = response.split(":", 4);
                int userId = Integer.parseInt(parts[1]);
                String roleStr = parts[2];
                String fullName = parts.length > 3 ? parts[3] : login;

                Role role = Role.valueOf(roleStr);
                User currentUser = new User();
                currentUser.setId(userId);
                currentUser.setLogin(login);
                currentUser.setFullName(fullName);
                currentUser.setRole(role);

                updateStatus("Вход выполнен успешно");
                Platform.runLater(() -> openMainWindow(currentUser, networkClient));

            } else {
                updateStatus(response.replace("ERROR:", "Ошибка:").trim());
                networkClient.close();
            }
        }).start();
    }

    @FXML
    private void handleRegister() {
        String login = loginField.getText().trim();
        String password = passwordField.getText().trim();

        if (login.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Введите логин и пароль для регистрации");
            return;
        }

        statusLabel.setText("Регистрация...");

        new Thread(() -> {
            networkClient = new NetworkClient();

            if (!networkClient.connect("localhost", 8080)) {
                updateStatus("Ошибка: сервер не запущен");
                return;
            }

            String response = networkClient.sendCommand("register " + login + " " + password);
            networkClient.close();

            if (response == null) {
                updateStatus("Ошибка: нет ответа от сервера");
            } else if (response.startsWith("OK:")) {
                updateStatus("Регистрация успешна. Теперь войдите");
            } else {
                updateStatus(response.replace("ERROR:", "Ошибка:").trim());
            }
        }).start();
    }

    private void updateStatus(String message) {
        Platform.runLater(() -> statusLabel.setText(message));
    }

    private void openMainWindow(User user, NetworkClient client) {
        try {
            String fxmlFile = "/client-view.fxml";
            String title = "Система учёта кредитных историй — " + user.getFullName();

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Scene scene = new Scene(loader.load());

            Object controller = loader.getController();
            if (controller instanceof ClientController) {
                ClientController clientController = (ClientController) controller;
                clientController.setCurrentUser(user);
                clientController.setNetworkClient(client);
                clientController.initializeData();
            }

            Stage stage = (Stage) loginField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle(title);
            stage.setMaximized(true);
            stage.setOnCloseRequest(e -> {
                if (client != null) client.close();
                System.exit(0);
            });

        } catch (IOException e) {
            updateStatus("Ошибка загрузки главного окна");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleClear() {
        loginField.clear();
        passwordField.clear();
        statusLabel.setText("Введите логин и пароль");
    }
}