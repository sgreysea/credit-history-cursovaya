package com.credithistory.client;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class AddCreditController {

    @FXML private Label titleLabel;
    @FXML private Label clientInfoLabel;
    @FXML private TextField amountField;
    @FXML private TextField termField;
    @FXML private TextField rateField;
    @FXML private Label monthlyPaymentLabel;
    @FXML private Label errorLabel;

    private Client client;
    private NetworkClient networkClient;
    private int currentUserId;
    private boolean saved = false;
    private int createdCreditId = 0;

    public void setClient(Client client) {
        this.client = client;
        clientInfoLabel.setText("Клиент: " + client.getFullName() + " (паспорт: " + client.getPassport() + ")");
        titleLabel.setText("Оформить кредит для " + client.getFullName());
    }

    public void setNetworkClient(NetworkClient networkClient) {
        this.networkClient = networkClient;
    }

    public void setCurrentUserId(int userId) {
        this.currentUserId = userId;
    }

    public boolean isSaved() {
        return saved;
    }

    public int getCreatedCreditId() {
        return createdCreditId;
    }

    @FXML
    private void handleCalculate() {
        try {
            BigDecimal amount = new BigDecimal(amountField.getText().trim());
            int term = Integer.parseInt(termField.getText().trim());
            BigDecimal rate = new BigDecimal(rateField.getText().trim());

            BigDecimal monthlyPayment = calculateMonthlyPayment(amount, rate, term);
            monthlyPaymentLabel.setText(String.format("%.2f BYN", monthlyPayment));
            errorLabel.setText("");
        } catch (NumberFormatException e) {
            errorLabel.setText("Проверьте правильность введённых чисел");
        }
    }

    private BigDecimal calculateMonthlyPayment(BigDecimal amount, BigDecimal yearlyRate, int months) {
        double monthlyRate = yearlyRate.doubleValue() / 100.0 / 12.0;
        double amountDouble = amount.doubleValue();

        if (monthlyRate == 0) {
            return amount.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
        }

        double payment = amountDouble * monthlyRate * Math.pow(1 + monthlyRate, months)
                / (Math.pow(1 + monthlyRate, months) - 1);

        return BigDecimal.valueOf(payment).setScale(2, RoundingMode.HALF_UP);
    }

    @FXML
    private void handleSave() {
        String amountStr = amountField.getText().trim();
        String termStr = termField.getText().trim();
        String rateStr = rateField.getText().trim();

        if (amountStr.isEmpty() || termStr.isEmpty() || rateStr.isEmpty()) {
            errorLabel.setText("Заполните все поля");
            return;
        }

        try {
            BigDecimal amount = new BigDecimal(amountStr);
            int term = Integer.parseInt(termStr);
            BigDecimal rate = new BigDecimal(rateStr);

            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                errorLabel.setText("Сумма должна быть больше нуля");
                return;
            }

            if (term <= 0) {
                errorLabel.setText("Срок должен быть больше нуля");
                return;
            }

            if (rate.compareTo(BigDecimal.ZERO) < 0) {
                errorLabel.setText("Ставка не может быть отрицательной");
                return;
            }

            // ООТПРАВКА НА СЕРВАК
            new Thread(() -> {
                String today = java.time.LocalDate.now().toString();
                String command = String.format("add_credit %d %s %d %s %s",
                        client.getId(), amount.toString(), term, rate.toString(), today);

                String response = networkClient.sendCommand(command);

                javafx.application.Platform.runLater(() -> {
                    if (response != null && response.startsWith("OK:")) {
                        String idStr = response.substring(3);
                        createdCreditId = Integer.parseInt(idStr);
                        saved = true;
                        closeWindow();
                    } else {
                        errorLabel.setText("Ошибка оформления кредита: " + response);
                    }
                });
            }).start();

        } catch (NumberFormatException e) {
            errorLabel.setText("Проверьте правильность введённых чисел");
        }
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) amountField.getScene().getWindow();
        stage.close();
    }
}