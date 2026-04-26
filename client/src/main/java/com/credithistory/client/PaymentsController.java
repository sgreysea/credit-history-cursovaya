package com.credithistory.client;

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
    @FXML private TableColumn<Payment, BigDecimal> penaltyColumn;
    @FXML private TableColumn<Payment, BigDecimal> totalColumn;
    @FXML private Label statusLabel;

    private Credit credit;
    private NetworkClient networkClient;
    private ObservableList<Payment> paymentsList = FXCollections.observableArrayList();

    public void setCredit(Credit credit) {
        this.credit = credit;
        titleLabel.setText("График платежей по кредиту #" + credit.getId());
        updateCreditInfo();
    }

    private void updateCreditInfo() {
        // Запросим актуальные данные о кредите с сервера
        new Thread(() -> {
            String response = networkClient.sendCommand("get_credit_info " + credit.getId());
            Platform.runLater(() -> {
                if (response != null && response.startsWith("OK:")) {
                    String[] parts = response.substring(3).split("\\|");
                    if (parts.length >= 4) {
                        creditInfoLabel.setText(String.format(
                                "Сумма: %s BYN | Осталось: %d мес. | Ставка: %s%%",
                                parts[0], Integer.parseInt(parts[1]), parts[2]
                        ));
                    }
                }
            });
        }).start();
    }

    public void setNetworkClient(NetworkClient networkClient) {
        this.networkClient = networkClient;
    }

    @FXML
    private void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        plannedDateColumn.setCellValueFactory(new PropertyValueFactory<>("plannedDate"));
        plannedAmountColumn.setCellValueFactory(new PropertyValueFactory<>("plannedAmount"));

        statusColumn.setCellValueFactory(cellData ->
                javafx.beans.binding.Bindings.createStringBinding(() ->
                        cellData.getValue().getStatus().getDisplayName()));

        actualDateColumn.setCellValueFactory(new PropertyValueFactory<>("actualDate"));
        actualAmountColumn.setCellValueFactory(new PropertyValueFactory<>("actualAmount"));

        penaltyColumn.setCellValueFactory(cellData ->
                javafx.beans.binding.Bindings.createObjectBinding(() ->
                        cellData.getValue().getPenalty()));

        totalColumn.setCellValueFactory(cellData ->
                javafx.beans.binding.Bindings.createObjectBinding(() ->
                        cellData.getValue().getTotalAmountWithPenalty()));

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
                                if (fields.length >= 6 && !fields[5].equals("null")) {
                                    payment.setActualAmount(new BigDecimal(fields[5]));
                                }
                                paymentsList.add(payment);
                            }
                        }
                    }
                    updateStatus("Загружено платежей: " + paymentsList.size());
                } else {
                    updateStatus("Ошибка загрузки: " + response);
                }
            });
        }).start();
    }

    @FXML
    private void handleRefresh() {
        loadPayments();
        updateCreditInfo();
    }

    @FXML
    private void handleMarkAsPaid() {
        Payment selected = paymentsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Выберите платёж");
            return;
        }

        if (selected.getStatus() == PaymentStatus.PAID) {
            showAlert("Этот платёж уже оплачен");
            return;
        }

        TextInputDialog dialog = new TextInputDialog(selected.getPlannedAmount().toString());
        dialog.setTitle("Оплата платежа");
        dialog.setHeaderText("Введите сумму оплаты");
        dialog.setContentText("Сумма:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            try {
                BigDecimal amount = new BigDecimal(result.get());

                new Thread(() -> {
                    String response = networkClient.sendCommand(
                            "mark_payment " + selected.getId() + " " + amount
                    );

                    Platform.runLater(() -> {
                        if (response != null && response.startsWith("OK:")) {
                            loadPayments();
                            updateCreditInfo();

                            // Показываем сообщение о результате
                            String[] parts = response.split(":");
                            if (parts.length > 1) {
                                updateStatus(parts[1]);
                            } else {
                                updateStatus("Платёж обработан");
                            }
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
    private void handleSkipPayment() {
        Payment selected = paymentsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Выберите платёж");
            return;
        }

        if (selected.getStatus() != PaymentStatus.PENDING) {
            showAlert("Можно пропустить только ожидаемый платёж");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Пропуск платежа");
        confirm.setHeaderText("Отметить платёж как пропущенный?");
        confirm.setContentText("Будет начислен штраф, кредитный рейтинг снизится.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                new Thread(() -> {
                    String serverResponse = networkClient.sendCommand(
                            "skip_payment " + selected.getId()
                    );

                    Platform.runLater(() -> {
                        if (serverResponse != null && serverResponse.startsWith("OK:")) {
                            loadPayments();
                            updateCreditInfo();
                            updateStatus("Платёж отмечен как пропущенный");
                        } else {
                            showAlert("Ошибка: " + serverResponse);
                        }
                    });
                }).start();
            }
        });
    }

    @FXML
    private void handleEarlyPayment() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Досрочное погашение");
        dialog.setHeaderText("Внесите сумму для досрочного погашения");
        dialog.setContentText("Сумма:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            try {
                BigDecimal amount = new BigDecimal(result.get());

                new Thread(() -> {
                    String response = networkClient.sendCommand(
                            "early_payment " + credit.getId() + " " + amount
                    );

                    Platform.runLater(() -> {
                        if (response != null && response.startsWith("OK:")) {
                            loadPayments();
                            updateCreditInfo();
                            String[] parts = response.split(":");
                            updateStatus(parts.length > 1 ? parts[1] : "Досрочное погашение выполнено");
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