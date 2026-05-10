package com.credithistory.client;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Pattern;

public class ClientDialogController {

    @FXML private Label titleLabel;
    @FXML private TextField fullNameField;
    @FXML private TextField passportField;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private TextField addressField;
    @FXML private TextField birthYearField;
    @FXML private Label errorLabel;

    private Client client;
    private boolean saved = false;
    private NetworkClient networkClient;
    private int currentUserId;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

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
            birthYearField.setText(client.getBirthYear() != null ? String.valueOf(client.getBirthYear()) : "");
        } else {
            titleLabel.setText("Добавить клиента");
            birthYearField.clear();
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
        String email = emailField.getText().trim();
        String address = addressField.getText().trim();
        String birthYearRaw = birthYearField.getText().trim();

        if (fullName.isEmpty() || passport.isEmpty()) {
            errorLabel.setText("ФИО и паспорт обязательны для заполнения");
            return;
        }
        if (!email.isEmpty() && !EMAIL_PATTERN.matcher(email).matches()) {
            errorLabel.setText("Введите корректный email");
            return;
        }

        Integer birthYear;
        if (birthYearRaw.isEmpty()) {
            if (client != null && client.getId() != 0) {
                if (client.getBirthYear() != null) {
                    birthYear = client.getBirthYear();
                } else {
                    errorLabel.setText("Укажите год рождения клиента");
                    return;
                }
            } else {
                errorLabel.setText("Укажите год рождения клиента");
                return;
            }
        } else {
            birthYear = resolveBirthYear(birthYearRaw);
            if (birthYear == null) {
                errorLabel.setText("Год рождения должен быть числом между 1900 и текущим годом");
                return;
            }
        }

        if (client == null) {
            client = new Client();
        }

        client.setFullName(fullName);
        client.setPassport(passport);
        client.setPhone(phone);
        client.setEmail(email);
        client.setAddress(address);
        client.setBirthYear(birthYear);
        client.setRegisteredBy(currentUserId);

        new Thread(() -> {
            String command;
            String yearStr = birthYear != null ? String.valueOf(birthYear) : "";
            String data = encodePayload(fullName, passport, phone, email, address, yearStr);
            if (client.getId() == 0) {
                command = "add_client " + data;
            } else {
                command = "update_client "
                        + encodePayload(String.valueOf(client.getId()), fullName, passport, phone, email, address, yearStr);
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

    private Integer resolveBirthYear(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            int y = Integer.parseInt(raw.trim());
            int cy = java.time.LocalDate.now().getYear();
            if (y < 1900 || y > cy) return null;
            return y;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String encodePayload(String... fields) {
        return Base64.getEncoder().encodeToString(String.join("|", fields).getBytes(StandardCharsets.UTF_8));
    }
}