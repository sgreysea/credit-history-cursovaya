package com.credithistory.client;

import com.credithistory.model.Client;
import com.credithistory.model.Credit;
import com.credithistory.model.User;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.util.Optional;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import javafx.stage.Modality;

public class ClientController {

    @FXML private Label userInfoLabel;
    @FXML private TextField searchField;
    @FXML private TableView<Client> clientsTable;
    @FXML private TableColumn<Client, Integer> idColumn;
    @FXML private TableColumn<Client, String> fullNameColumn;
    @FXML private TableColumn<Client, String> passportColumn;
    @FXML private TableColumn<Client, String> phoneColumn;
    @FXML private TableColumn<Client, Integer> ratingColumn;

    @FXML private Label selectedClientLabel;
    @FXML private TableView<Credit> creditsTable;
    @FXML private TableColumn<Credit, Integer> creditIdColumn;
    @FXML private TableColumn<Credit, BigDecimal> creditAmountColumn;
    @FXML private TableColumn<Credit, Integer> creditTermColumn;
    @FXML private TableColumn<Credit, BigDecimal> creditRateColumn;
    @FXML private TableColumn<Credit, LocalDate> creditDateColumn;
    @FXML private TableColumn<Credit, String> creditStatusColumn;

    @FXML private Label statusLabel;

    private NetworkClient networkClient;  // ← ПЕРЕДАЁТСЯ из LoginController
    private User currentUser;
    private ObservableList<Client> clientsList = FXCollections.observableArrayList();
    private ObservableList<Credit> creditsList = FXCollections.observableArrayList();

    public void setCurrentUser(User user) {
        this.currentUser = user;
        userInfoLabel.setText("Сотрудник: " + user.getFullName() + " (" + user.getRole().getDisplayName() + ")");
    }

    public void setNetworkClient(NetworkClient networkClient) {
        this.networkClient = networkClient;
    }

    public void initializeData() {
        loadClients();
    }

    @FXML
    private void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        fullNameColumn.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        passportColumn.setCellValueFactory(new PropertyValueFactory<>("passport"));
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        ratingColumn.setCellValueFactory(cellData ->
                javafx.beans.binding.Bindings.createObjectBinding(() -> 500)
        );

        clientsTable.setItems(clientsList);

        creditIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        creditAmountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        creditTermColumn.setCellValueFactory(new PropertyValueFactory<>("termMonths"));
        creditRateColumn.setCellValueFactory(new PropertyValueFactory<>("interestRate"));
        creditDateColumn.setCellValueFactory(new PropertyValueFactory<>("issueDate"));
        creditStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        creditsTable.setItems(creditsList);

        clientsTable.getSelectionModel().selectedItemProperty().addListener((obs, old, newClient) -> {
            if (newClient != null) {
                selectedClientLabel.setText("Кредиты клиента: " + newClient.getFullName());
                loadCreditsForClient(newClient.getId());
            }
        });
    }

    private void loadClients() {
        if (networkClient == null || !networkClient.isConnected()) {
            updateStatus("Нет подключения к серверу");
            return;
        }

        new Thread(() -> {
            String response = networkClient.sendCommand("get_clients");

            Platform.runLater(() -> {
                clientsList.clear();
                if (response != null && response.startsWith("OK:")) {
                    String data = response.substring(3);
                    if (!data.isEmpty()) {
                        String[] items = data.split(";");
                        for (String item : items) {
                            String[] fields = item.split("\\|");
                            if (fields.length >= 4) {
                                Client client = new Client();
                                client.setId(Integer.parseInt(fields[0]));
                                client.setFullName(fields[1]);
                                client.setPassport(fields[2]);
                                client.setPhone(fields[3]);
                                clientsList.add(client);
                            }
                        }
                    }
                    updateStatus("Загружено клиентов: " + clientsList.size());
                } else {
                    updateStatus("Ошибка загрузки клиентов: " + response);
                }
            });
        }).start();
    }

    private void loadCreditsForClient(int clientId) {
        if (networkClient == null || !networkClient.isConnected()) {
            return;
        }

        new Thread(() -> {
            String response = networkClient.sendCommand("get_credits " + clientId);

            Platform.runLater(() -> {
                creditsList.clear();
                if (response != null && response.startsWith("OK:")) {
                    String data = response.substring(3);
                    if (!data.isEmpty()) {
                        String[] items = data.split(";");
                        for (String item : items) {
                            String[] fields = item.split("\\|");
                            if (fields.length >= 7) {
                                Credit credit = new Credit();
                                credit.setId(Integer.parseInt(fields[0]));
                                credit.setClientId(Integer.parseInt(fields[1]));
                                credit.setAmount(new BigDecimal(fields[2]));
                                credit.setTermMonths(Integer.parseInt(fields[3]));
                                credit.setInterestRate(new BigDecimal(fields[4]));
                                credit.setIssueDate(LocalDate.parse(fields[5]));
                                credit.setStatus(com.credithistory.model.CreditStatus.valueOf(fields[6]));
                                creditsList.add(credit);
                            }
                        }
                    }
                }
            });
        }).start();
    }

    @FXML
    private void handleRefresh() {
        loadClients();
    }

    @FXML
    private void handleSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            loadClients();
            return;
        }

    }

    @FXML
    private void handleResetSearch() {
        searchField.clear();
        loadClients();
    }

    @FXML
    private void handleLogout() {
        if (networkClient != null) {
            networkClient.sendCommand("logout");
            networkClient.close();
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) clientsTable.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Вход в систему");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAddClient() {
        showClientDialog(null);
    }

    @FXML
    private void handleEditClient() {
        Client selected = clientsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            showClientDialog(selected);
        } else {
            showAlert("Выберите клиента для редактирования");
        }
    }

    @FXML
    private void handleDeleteClient() {
        Client selected = clientsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Выберите клиента для удаления");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Подтверждение");
        confirm.setHeaderText("Удалить клиента?");
        confirm.setContentText("Вы уверены, что хотите удалить клиента " + selected.getFullName() + "?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            new Thread(() -> {
                String response = networkClient.sendCommand("delete_client " + selected.getId());

                Platform.runLater(() -> {
                    if (response != null && response.startsWith("OK:")) {
                        loadClients();
                        updateStatus("Клиент удалён");
                    } else {
                        showAlert("Ошибка удаления: " + response);
                    }
                });
            }).start();
        }
    }

    @FXML
    private void handleShowCreditHistory() {
        Client selected = clientsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Выберите клиента");
            return;
        }
        showAlert("Кредитная история клиента: " + selected.getFullName());
    }

    @FXML
    private void handleCloseCredit() {
        Credit selected = creditsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Выберите кредит для закрытия");
            return;
        }
        showAlert("Закрытие кредита #" + selected.getId());
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
    private void showClientDialog(Client client) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/client-dialog.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle(client == null ? "Добавить клиента" : "Редактировать клиента");
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(clientsTable.getScene().getWindow());

            ClientDialogController controller = loader.getController();
            controller.setNetworkClient(networkClient);
            controller.setCurrentUserId(currentUser.getId());
            controller.setClient(client);

            stage.showAndWait();

            if (controller.isSaved()) {
                loadClients();  // Обновляем таблицу
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Ошибка открытия диалога");
        }
    }
    @FXML
    private void handleAddCredit() {
        Client selected = clientsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Выберите клиента для оформления кредита");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/add-credit.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle("Оформить кредит");
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(clientsTable.getScene().getWindow());

            AddCreditController controller = loader.getController();
            controller.setClient(selected);
            controller.setNetworkClient(networkClient);
            controller.setCurrentUserId(currentUser.getId());

            stage.showAndWait();

            if (controller.isSaved()) {
                loadCreditsForClient(selected.getId());
                updateStatus("Кредит оформлен, ID: " + controller.getCreatedCreditId());
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Ошибка открытия диалога");
        }
    }
    @FXML
    private void handleShowPayments() {
        Credit selected = creditsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Выберите кредит для просмотра платежей");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/payments-view.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle("График платежей");
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(clientsTable.getScene().getWindow());

            PaymentsController controller = loader.getController();
            controller.setCredit(selected);
            controller.setNetworkClient(networkClient);
            controller.loadPayments();

            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Ошибка открытия окна платежей");
        }
    }
}