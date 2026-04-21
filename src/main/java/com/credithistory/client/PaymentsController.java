package com.credithistory.client;

import com.credithistory.model.Credit;
import com.credithistory.model.Payment;
import com.credithistory.model.PaymentStatus;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

public class PaymentsController {

    @FXML private Label titleLabel;
    @FXML private Label creditInfoLabel;
    @FXML private TableView<Payment> paymentsTable;
    @FXML private TableColumn<Payment, Integer> idColumn;
    @FXML private TableColumn<Payment, LocalDate> plannedDateColumn;
    @FXML private TableColumn<Payment, BigDecimal> plannedAmountColumn;
    @FXML private TableColumn<Payment, String> statusColumn;
    @FXML private TableColumn<Payment, LocalDate> actualDateColumn;
    @FXML private TableColumn<Payment, BigDecimal> actualAmountColumn;
    @FXML private Label statusLabel;

    private Credit credit;
    private NetworkClient networkClient;
    private ObservableList<Payment> paymentsList = FXCollections.observableArrayList();

    public void setCredit(Credit credit) {
        this.credit = credit;
        titleLabel.setText("График платежей по кредиту #" + credit.getId());
        creditInfoLabel.setText(String.format("Сумма: %.2f BYN | Срок: %d мес. | Ставка: %.2f%%",
                credit.getAmount(), credit.getTermMonths(), credit.getInterestRate()));
    }

    public void setNetworkClient(NetworkClient networkClient) {
        this.networkClient = networkClient;
    }

    @FXML
    private void initialize() {
        // Настройка колонок
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        plannedDateColumn.setCellValueFactory(new PropertyValueFactory<>("plannedDate"));
        plannedAmountColumn.setCellValueFactory(new PropertyValueFactory<>("plannedAmount"));
        statusColumn.setCellValueFactory(cellData ->
                javafx.beans.binding.Bindings.createStringBinding(() ->
                        cellData.getValue().getStatus().getDisplayName()));
        actualDateColumn.setCellValueFactory(new PropertyValueFactory<>("actualDate"));
        actualAmountColumn.setCellValueFactory(new PropertyValueFactory<>("actualAmount"));

        paymentsTable.setItems(paymentsList);
    }

    public void loadPayments() {
        if (networkClient == null || !networkClient.isConnected()) {
            updateStatus("Нет подключения к серверу");
            return;
        }

        new Thread(() -> {
            String response = networkClient.sendCommand("get_payments " + credit.getId());

            Platform.runLater(() -> {
                paymentsList.clear();
                if (response != null && response.startsWith("OK:")) {
                    String data = response.substring(3);
                    if (!data.isEmpty()) {
                        String[] items = data.split(";");
                        for (String item : items) {
                            String[] fields = item.split("\\|");
                            if (fields.length >= 4) {
                                Payment payment = new Payment();
                                payment.setId(Integer.parseInt(fields[0]));
                                payment.setPlannedDate(LocalDate.parse(fields[1]));
                                payment.setPlannedAmount(new BigDecimal(fields[2]));
                                payment.setStatus(PaymentStatus.valueOf(fields[3]));
                                if (fields.length >= 5 && !fields[4].equals("-")) {
                                    payment.setActualDate(LocalDate.parse(fields[4]));
                                }
                                paymentsList.add(payment);
                            }
                        }
                    }
                    updateStatus("Загружено платежей: " + paymentsList.size());
                } else {
                    updateStatus("Ошибка загрузки платежей: " + response);
                }

            });
        }).start();
    }

    @FXML
    private void handleRefresh() {
        loadPayments();
    }

    @FXML
    private void handleMarkAsPaid() {
        Payment selected = paymentsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Выберите платёж для отметки");
            return;
        }

        if (selected.getStatus() == PaymentStatus.PAID) {
            showAlert("Этот платёж уже оплачен");
            return;
        }

        // Диалог ввода суммы
        TextInputDialog dialog = new TextInputDialog(selected.getPlannedAmount().toString());
        dialog.setTitle("Оплата платежа");
        dialog.setHeaderText("Отметить платёж как оплаченный");
        dialog.setContentText("Введите сумму оплаты:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            try {
                BigDecimal amount = new BigDecimal(result.get());

                new Thread(() -> {
                    String response = networkClient.sendCommand("mark_payment " + selected.getId() + " " + amount);

                    Platform.runLater(() -> {
                        if (response != null && response.startsWith("OK:")) {
                            loadPayments();
                            updateStatus("Платёж отмечен как оплаченный");
                        } else {
                            showAlert("Ошибка: " + response);
                        }
                    });
                }).start();
            } catch (NumberFormatException e) {
                showAlert("Неверный формат суммы");
            }
        }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) paymentsTable.getScene().getWindow();
        stage.close();
    }

    private void updateStatus(String message) {
        Platform.runLater(() -> statusLabel.setText(message));
    }

    private void showAlert(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Информация");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

}