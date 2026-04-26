package com.credithistory.client;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class ClientDialogController {

    @FXML private Label titleLabel;
    @FXML private TextField fullNameField;
    @FXML private TextField passportField;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private TextField addressField;
    @FXML private Label errorLabel;

    private Client client;
    private boolean saved = false;
    private NetworkClient networkClient;
    private int currentUserId;

    public void setNetworkClient(NetworkClient networkClient) {
        this.networkClient = networkClient;
    }

    public void setCurrentUserId(int userId) {
        this.currentUserId = userId;
    }

    public void setClient(Client client) {
        this.client = client;
        if (client != null) {
            titleLabel.setText("Редактировать клиента");
            fullNameField.setText(client.getFullName());
            passportField.setText(client.getPassport());
            phoneField.setText(client.getPhone() != null ? client.getPhone() : "");
            emailField.setText(client.getEmail() != null ? client.getEmail() : "");
            addressField.setText(client.getAddress() != null ? client.getAddress() : "");
        } else {
            titleLabel.setText("Добавить клиента");
        }
    }

    public boolean isSaved() {
        return saved;
    }

    public Client getClient() {
        return client;
    }

    @FXML
    private void handleSave() {
        String fullName = fullNameField.getText().trim();
        String passport = passportField.getText().trim();
        String phone = phoneField.getText().trim();

        if (fullName.isEmpty() || passport.isEmpty()) {
            errorLabel.setText("ФИО и паспорт обязательны для заполнения");
            return;
        }

        if (client == null) {
            client = new Client();
        }

        client.setFullName(fullName);
        client.setPassport(passport);
        client.setPhone(phone);
        client.setEmail(emailField.getText().trim());
        client.setAddress(addressField.getText().trim());
        client.setRegisteredBy(currentUserId);

        // отправка на сервак
        new Thread(() -> {
            String command;
            if (client.getId() == 0) {
                command = "add_client " + fullName + " " + passport + " " + phone;
            } else {
                command = "update_client " + client.getId() + " " + fullName + " " + passport + " " + phone;
            }

            String response = networkClient.sendCommand(command);

            javafx.application.Platform.runLater(() -> {
                if (response != null && response.startsWith("OK:")) {
                    if (client.getId() == 0) {
                        String idStr = response.substring(3);
                        client.setId(Integer.parseInt(idStr));
                    }
                    saved = true;
                    closeWindow();
                } else {
                    errorLabel.setText("Ошибка сохранения: " + response);
                }
            });
        }).start();
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) fullNameField.getScene().getWindow();
        stage.close();
    }
}