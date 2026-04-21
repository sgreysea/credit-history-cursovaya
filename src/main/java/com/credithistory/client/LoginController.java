package com.credithistory.client;

import com.credithistory.model.Role;
import com.credithistory.model.User;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
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
                updateStatus("Ошибка: сервер не запущен!");
                return;
            }

            updateStatus("Проверка учетных данных...");
            String response = networkClient.sendCommand("login " + login + " " + password);
            networkClient.close();

            if (response == null) {
                updateStatus("Ошибка: нет ответа от сервера");
                return;
            }

            if (response.startsWith("OK:")) {
                String[] parts = response.split(":");
                String roleStr = parts[1];
                String fullName = parts.length > 2 ? parts[2] : login;

                Role role = Role.valueOf(roleStr);

                // Создаём объект пользователя
                User currentUser = new User();
                currentUser.setLogin(login);
                currentUser.setFullName(fullName);
                currentUser.setRole(role);

                updateStatus("Вход выполнен успешно!");

                // Открываем главное окно в зависимости от роли
                Platform.runLater(() -> openMainWindow(currentUser));

            } else if (response.startsWith("ERROR:")) {
                updateStatus("Ошибка: " + response.substring(6));
            } else {
                updateStatus("Неверный логин или пароль");
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
                updateStatus("Ошибка: сервер не запущен!");
                return;
            }

            String response = networkClient.sendCommand("register " + login + " " + password);
            networkClient.close();

            if (response == null) {
                updateStatus("Ошибка: нет ответа от сервера");
            } else if (response.startsWith("OK:")) {
                updateStatus("Регистрация успешна! Теперь войдите.");
            } else if (response.startsWith("ERROR:")) {
                updateStatus("Ошибка: " + response.substring(6));
            } else {
                updateStatus("Ошибка регистрации");
            }
        }).start();
    }

    private void updateStatus(String message) {
        Platform.runLater(() -> statusLabel.setText(message));
    }

    private void openMainWindow(User user) {
        try {
            String fxmlFile;
            String title;

            // Выбираем FXML в зависимости от роли
            if (user.getRole() == Role.ADMIN || user.getRole() == Role.SUPER_ADMIN) {
                fxmlFile = "/admin-view.fxml";
                title = "Система учета кредитных историй — Администратор";
            } else {
                fxmlFile = "/client-view.fxml";
                title = "Система учета кредитных историй — Сотрудник банка";
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Scene scene = new Scene(loader.load());

            // Передаём пользователя в контроллер главного окна
            Object controller = loader.getController();
            if (controller instanceof ClientController) {
                ((ClientController) controller).setCurrentUser(user);
                // Загружаем список клиентов сразу после открытия
                Platform.runLater(() -> {
                    try {
                        // Вызываем метод refresh через небольшой delay, чтобы окно успело открыться
                        java.lang.reflect.Method method = controller.getClass().getMethod("handleRefresh");
                        method.invoke(controller);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            Stage stage = (Stage) loginField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle(title);
            stage.setMaximized(true);
            stage.setOnCloseRequest(e -> System.exit(0));

        } catch (IOException e) {
            updateStatus("Ошибка загрузки главного окна");
            e.printStackTrace();
        }
    }
}