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
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

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

    private NetworkClient networkClient;
    private User currentUser;
    private ObservableList<Client> clientsList = FXCollections.observableArrayList();
    private ObservableList<Credit> creditsList = FXCollections.observableArrayList();

    public void setCurrentUser(User user) {
        this.currentUser = user;
        userInfoLabel.setText("Сотрудник: " + user.getFullName() + " (" + user.getRole().getDisplayName() + ")");
    }

    @FXML
    private void initialize() {
        // Настройка колонок таблицы клиентов
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        fullNameColumn.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        passportColumn.setCellValueFactory(new PropertyValueFactory<>("passport"));
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        ratingColumn.setCellValueFactory(cellData -> {
            // Здесь должен быть рейтинг, пока ставим 0
            return javafx.beans.binding.Bindings.createObjectBinding(() -> 500);
        });

        clientsTable.setItems(clientsList);

        // Настройка колонок таблицы кредитов
        creditIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        creditAmountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        creditTermColumn.setCellValueFactory(new PropertyValueFactory<>("termMonths"));
        creditRateColumn.setCellValueFactory(new PropertyValueFactory<>("interestRate"));
        creditDateColumn.setCellValueFactory(new PropertyValueFactory<>("issueDate"));
        creditStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        creditsTable.setItems(creditsList);

        // Обработчик выбора клиента
        clientsTable.getSelectionModel().selectedItemProperty().addListener((obs, old, newClient) -> {
            if (newClient != null) {
                selectedClientLabel.setText("Кредиты клиента: " + newClient.getFullName());
                loadCreditsForClient(newClient.getId());
            }
        });
    }

    private void loadClients() {
        new Thread(() -> {
            networkClient = new NetworkClient();
            if (!networkClient.connect("localhost", 8080)) {
                updateStatus("Ошибка подключения к серверу");
                return;
            }

            String response = networkClient.sendCommand("get_clients");
            networkClient.close();

            Platform.runLater(() -> {
                clientsList.clear();
                if (response != null && response.startsWith("OK:")) {
                    String data = response.substring(3);
                    if (!data.isEmpty()) {
                        String[] items = data.split(";");
                        for (String item : items) {
                            String[] fields = item.split("\\|");
                            Client client = new Client();
                            client.setId(Integer.parseInt(fields[0]));
                            client.setFullName(fields[1]);
                            client.setPassport(fields[2]);
                            client.setPhone(fields[3]);
                            clientsList.add(client);
                        }
                    }
                    updateStatus("Загружено клиентов: " + clientsList.size());
                } else {
                    updateStatus("Ошибка загрузки клиентов");
                }
            });
        }).start();
    }

    private void loadCreditsForClient(int clientId) {
        // Заглушка, будет реализована позже
        creditsList.clear();
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
        // Реализация поиска
    }

    @FXML
    private void handleResetSearch() {
        searchField.clear();
        loadClients();
    }

    @FXML
    private void handleLogout() {
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

    private void showClientDialog(Client client) {
        // Заглушка для диалога
        showAlert("Функция в разработке");
    }

    @FXML
    private void handleDeleteClient() {
        Client selected = clientsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Выберите клиента для удаления");
            return;
        }
        // Реализация удаления
    }

    @FXML
    private void handleShowCreditHistory() {
        Client selected = clientsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Выберите клиента");
            return;
        }
        // Реализация просмотра истории
    }

    @FXML
    private void handleAddCredit() {
        Client selected = clientsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Выберите клиента для оформления кредита");
            return;
        }
        // Реализация оформления кредита
    }

    @FXML
    private void handleShowPayments() {
        // Реализация просмотра платежей
    }

    @FXML
    private void handleCloseCredit() {
        // Реализация закрытия кредита
    }

    private void updateStatus(String message) {
        Platform.runLater(() -> statusLabel.setText(message));
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Информация");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}