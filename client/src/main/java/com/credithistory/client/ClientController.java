package com.credithistory.client;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.Base64;

public class ClientController {

    @FXML private Label userInfoLabel;
    @FXML private TextField searchField;
    @FXML private TableView<Client> clientsTable;
    @FXML private TableColumn<Client, Integer> idColumn;
    @FXML private TableColumn<Client, String> fullNameColumn;
    @FXML private TableColumn<Client, String> passportColumn;
    @FXML private TableColumn<Client, String> phoneColumn;
    @FXML private TableColumn<Client, Integer> creditCountColumn;
    @FXML private TableColumn<Client, String> ratingColumn;
    @FXML private Button addCreditButton;
    @FXML private Button closeCreditButton;
    @FXML private Button showPaymentsButton;
    @FXML private Button statsButton;
    @FXML private Button addUserButton;

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
    private Map<Integer, String> employeeNames = new HashMap<>();
    private Map<Integer, String> clientRatings = new HashMap<>();

    public void setCurrentUser(User user) {
        this.currentUser = user;
        userInfoLabel.setText("Сотрудник: " + user.getFullName() + " (" + user.getRole().getDisplayName() + ")");

        // Супер-админ не может оформлять/закрывать кредиты и график платежей
        if (user.getRole() == Role.SUPER_ADMIN) {
            addCreditButton.setManaged(false);
            addCreditButton.setVisible(false);
            closeCreditButton.setManaged(false);
            closeCreditButton.setVisible(false);
            showPaymentsButton.setManaged(false);
            showPaymentsButton.setVisible(false);
            addUserButton.setVisible(true);
            addUserButton.setManaged(true);
        } else {
            addUserButton.setVisible(false);
            addUserButton.setManaged(false);
        }

        statsButton.setManaged(user.getRole() == Role.SUPER_ADMIN);
        statsButton.setVisible(user.getRole() == Role.SUPER_ADMIN);
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
        creditCountColumn.setCellValueFactory(new PropertyValueFactory<>("creditCount"));

        // Колонка "Добавил сотрудник"
        TableColumn<Client, String> addedByColumn = new TableColumn<>("Добавил сотрудник");
        addedByColumn.setCellValueFactory(cellData -> {
            Client c = cellData.getValue();
            String name = employeeNames.getOrDefault(c.getRegisteredBy(), String.valueOf(c.getRegisteredBy()));
            return javafx.beans.binding.Bindings.createStringBinding(() -> name);
        });
        clientsTable.getColumns().add(addedByColumn);

        // Колонка с переходом в окно кредитного рейтинга
        TableColumn<Client, Void> bioColumn = new TableColumn<>("рейтинг");
        bioColumn.setCellFactory(col -> {
            TableCell<Client, Void> cell = new TableCell<>() {
                private final Button btn = new Button("📊 открыть");
                {
                    btn.setOnAction(e -> {
                        Client client = getTableView().getItems().get(getIndex());
                        showClientBio(client.getId());
                    });
                }
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : btn);
                }
            };
            return cell;
        });
        clientsTable.getColumns().add(bioColumn);

        // Колонка "Рейтинг" — цветная буква из кэша clientRatings
        ratingColumn.setCellValueFactory(cellData -> {
            Client c = cellData.getValue();
            String letter = clientRatings.getOrDefault(c.getId(), "?");
            return javafx.beans.binding.Bindings.createStringBinding(() -> letter);
        });
        ratingColumn.setCellFactory(col -> new TableCell<Client, String>() {
            @Override
            protected void updateItem(String letter, boolean empty) {
                super.updateItem(letter, empty);
                getStyleClass().removeAll("rating-a", "rating-b", "rating-c", "rating-d", "rating-e", "rating-unknown");
                if (empty || letter == null) {
                    setText(null);
                    setStyle("");
                } else {
                    String styleClass = switch (letter) {
                        case "A" -> "rating-a";
                        case "B" -> "rating-b";
                        case "C" -> "rating-c";
                        case "D" -> "rating-d";
                        case "E" -> "rating-e";
                        default -> "rating-unknown";
                    };
                    setText(letter);
                    getStyleClass().add(styleClass);
                    setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
                }
            }
        });

        clientsTable.setItems(clientsList);

        creditIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        creditAmountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        creditTermColumn.setCellValueFactory(new PropertyValueFactory<>("termMonths"));
        creditRateColumn.setCellValueFactory(new PropertyValueFactory<>("interestRate"));
        creditDateColumn.setCellValueFactory(new PropertyValueFactory<>("issueDate"));
        creditStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Колонка "Добавил кредит"
        TableColumn<Credit, String> creditAddedByColumn = new TableColumn<>("Добавил кредит");
        creditAddedByColumn.setCellValueFactory(cellData -> {
            Credit c = cellData.getValue();
            String name = employeeNames.getOrDefault(c.getUserId(), String.valueOf(c.getUserId()));
            return javafx.beans.binding.Bindings.createStringBinding(() -> name);
        });
        creditsTable.getColumns().add(creditAddedByColumn);

        creditsTable.setItems(creditsList);

        clientsTable.getSelectionModel().selectedItemProperty().addListener((obs, old, newClient) -> {
            if (newClient != null) {
                selectedClientLabel.setText("кредиты клиента: " + newClient.getFullName());
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
                employeeNames.clear();
                clientRatings.clear();
                if (response != null && response.startsWith("OK:")) {
                    String data = response.substring(3);
                    if (!data.isEmpty()) {
                        String[] items = data.split(";");
                        for (String item : items) {
                            String[] fields = item.split("\\|");
                            if (!item.isBlank() && fields.length >= 8) {
                                Client client = new Client();
                                client.setId(Integer.parseInt(fields[0]));
                                client.setFullName(fields[1]);
                                client.setPassport(fields[2]);
                                client.setPhone(fields[3]);
                                client.setRegisteredBy(Integer.parseInt(fields[4]));
                                client.setRatingLetter(fields[7]);
                                employeeNames.put(Integer.parseInt(fields[4]), fields[5]);
                                clientRatings.put(client.getId(), fields[7]);
                                if (fields.length >= 10) {
                                    if (!fields[8].isEmpty()) {
                                        try {
                                            client.setBirthYear(Integer.parseInt(fields[8]));
                                        } catch (NumberFormatException ignored) {
                                            client.setBirthYear(null);
                                        }
                                    }
                                    try {
                                        client.setCreditCount(Integer.parseInt(fields[9]));
                                    } catch (NumberFormatException ignored) {
                                        client.setCreditCount(0);
                                    }
                                } else {
                                    client.setCreditCount(0);
                                }
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
        if (networkClient == null || !networkClient.isConnected()) return;

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
                            if (fields.length >= 9) {
                                Credit credit = new Credit();
                                credit.setId(Integer.parseInt(fields[0]));
                                credit.setClientId(Integer.parseInt(fields[1]));
                                credit.setAmount(new BigDecimal(fields[2]));
                                credit.setTermMonths(Integer.parseInt(fields[3]));
                                credit.setInterestRate(new BigDecimal(fields[4]));
                                credit.setIssueDate(LocalDate.parse(fields[5]));
                                credit.setStatus(CreditStatus.valueOf(fields[6]));
                                credit.setUserId(Integer.parseInt(fields[7]));
                                employeeNames.put(Integer.parseInt(fields[7]), fields[8]);
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
        // Заглушка — поиск на стороне клиента
        String query = searchField.getText().trim().toLowerCase();
        if (query.isEmpty()) {
            loadClients();
            return;
        }
        clientsList.clear();
        // TODO: реализовать поиск через сервер
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
    private void handleAddUser() {
        showUsersManagementDialog();
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
        confirm.setContentText("Вы уверены?");
        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                new Thread(() -> {
                    String response = networkClient.sendCommand("delete_client " + selected.getId());
                    Platform.runLater(() -> {
                        if (response != null && response.startsWith("OK:")) {
                            loadClients();
                            updateStatus("Клиент удалён");
                        } else {
                            showAlert("Ошибка: " + response);
                        }
                    });
                }).start();
            }
        });
    }

    @FXML
    private void handleShowCreditHistory() {
        Client selected = clientsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Выберите клиента");
            return;
        }
        showClientBio(selected.getId());
    }

    @FXML
    private void handleCloseCredit() {
        Credit selected = creditsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Выберите кредит для закрытия");
            return;
        }
        if (selected.getStatus() == CreditStatus.CLOSED) {
            showAlert("Кредит уже закрыт");
            return;
        }
        new Thread(() -> {
            String response = networkClient.sendCommand("close_credit " + selected.getId());
            Platform.runLater(() -> {
                if (response != null && response.startsWith("OK:")) {
                    loadCreditsForClient(selected.getClientId());
                    updateStatus("Кредит закрыт");
                } else {
                    showAlert("Ошибка: " + response);
                }
            });
        }).start();
    }

    private void showClientBio(int clientId) {
        if (networkClient == null || !networkClient.isConnected()) {
            showAlert("Нет подключения");
            return;
        }
        new Thread(() -> {
                    String resp = networkClient.sendCommand("get_client_bio " + clientId);
            Platform.runLater(() -> {
                if (resp != null && resp.startsWith("OK:")) {
                    try {
                        String[] parts = resp.substring(3).split("\\|", -1);
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/client-bio.fxml"));
                        Stage stage = new Stage();
                        stage.setScene(new Scene(loader.load()));
                        ClientBioController ctrl = loader.getController();
                        ctrl.setClientData(parts);
                        stage.setTitle("Кредитный рейтинг");
                        stage.initModality(Modality.WINDOW_MODAL);
                        stage.initOwner(clientsTable.getScene().getWindow());
                        stage.show();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    showAlert("Ошибка: " + resp);
                }
            });
        }).start();
    }

    @FXML
    private void handleShowStats() {
        if (networkClient == null || !networkClient.isConnected()) {
            showAlert("Нет подключения");
            return;
        }
        new Thread(() -> {
            String response = networkClient.sendCommand("get_employee_stats");
            Platform.runLater(() -> {
                if (response != null && response.startsWith("OK:")) {
                    String[] rows = response.substring(3).split(";");
                    TableView<String[]> table = new TableView<>();
                    String[] cols = {"ФИО", "клиентов добавлено", "кредитов оформлено"};
                    for (int i = 0; i < cols.length; i++) {
                        final int idx = i + 1;
                        TableColumn<String[], String> col = new TableColumn<>(cols[i]);
                        col.setCellValueFactory(cell -> javafx.beans.binding.Bindings.createStringBinding(() -> cell.getValue()[idx]));
                        table.getColumns().add(col);
                    }
                    ObservableList<String[]> list = FXCollections.observableArrayList();
                    for (String row : rows) {
                        if (!row.isBlank()) {
                            String[] colsData = row.split("\\|", -1);
                            if (colsData.length >= 4) list.add(colsData);
                        }
                    }
                    table.setItems(list);
                    Stage stage = new Stage();
                    stage.setTitle("статистика сотрудников банка");
                    stage.initModality(Modality.WINDOW_MODAL);
                    stage.initOwner(clientsTable.getScene().getWindow());
                    VBox vbox = new VBox(12, new Label("статистика по действиям сотрудников"), table);
                    vbox.setPadding(new Insets(10));
                    stage.setScene(new Scene(vbox, 600, 400));
                    stage.show();
                } else {
                    showAlert("Ошибка: " + response);
                }
            });
        }).start();
    }

    private void updateStatus(String msg) {
        Platform.runLater(() -> statusLabel.setText(msg));
    }

    private void showAlert(String msg) {
        Platform.runLater(() -> {
            Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
            a.setHeaderText(null);
            a.showAndWait();
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
            ClientDialogController ctrl = loader.getController();
            ctrl.setNetworkClient(networkClient);
            ctrl.setCurrentUserId(currentUser.getId());
            ctrl.setClient(client);
            stage.showAndWait();
            if (ctrl.isSaved()) loadClients();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showUsersManagementDialog() {
        TableView<String[]> table = new TableView<>();
        ObservableList<String[]> users = FXCollections.observableArrayList();
        String[] headers = {"ID", "Логин", "ФИО", "Роль", "Активен"};
        for (int i = 0; i < headers.length; i++) {
            final int idx = i;
            TableColumn<String[], String> col = new TableColumn<>(headers[i]);
            col.setCellValueFactory(cell ->
                    javafx.beans.binding.Bindings.createStringBinding(() -> cell.getValue()[idx]));
            col.setPrefWidth(i == 2 ? 220 : 120);
            table.getColumns().add(col);
        }
        table.setItems(users);

        Runnable loadUsers = () -> new Thread(() -> {
            String response = networkClient.sendCommand("get_users");
            Platform.runLater(() -> {
                users.clear();
                if (response != null && response.startsWith("OK:")) {
                    String body = response.substring(3);
                    if (!body.isBlank()) {
                        for (String row : body.split(";")) {
                            String[] fields = row.split("\\|", -1);
                            if (fields.length >= 5) users.add(fields);
                        }
                    }
                } else {
                    showAlert("Ошибка загрузки пользователей: " + response);
                }
            });
        }).start();

        Button refreshBtn = new Button("Обновить");
        refreshBtn.setOnAction(e -> loadUsers.run());

        Button addBtn = new Button("Добавить");
        addBtn.setOnAction(e -> showAddUserDialog(loadUsers));

        Button editBtn = new Button("Редактировать");
        editBtn.setOnAction(e -> {
            String[] selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert("Выберите сотрудника");
                return;
            }
            showEditUserDialog(selected, loadUsers);
        });

        Button deleteBtn = new Button("Удалить");
        deleteBtn.setOnAction(e -> {
            String[] selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert("Выберите сотрудника");
                return;
            }
            int userId = Integer.parseInt(selected[0]);
            new Thread(() -> {
                String response = networkClient.sendCommand("delete_user " + userId);
                Platform.runLater(() -> {
                    if (response != null && response.startsWith("OK:")) {
                        loadUsers.run();
                    } else {
                        showAlert("Ошибка удаления: " + response);
                    }
                });
            }).start();
        });

        HBox toolbar = new HBox(10, refreshBtn, addBtn, editBtn, deleteBtn);
        VBox root = new VBox(10, new Label("Управление сотрудниками банка"), toolbar, table);
        root.setPadding(new Insets(10));

        Stage stage = new Stage();
        stage.setTitle("Сотрудники банка");
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(clientsTable.getScene().getWindow());
        stage.setScene(new Scene(root, 760, 420));
        stage.show();
        loadUsers.run();
    }

    private void showAddUserDialog(Runnable onSaved) {
        TextInputDialog loginDialog = new TextInputDialog();
        loginDialog.setTitle("Новый сотрудник");
        loginDialog.setHeaderText("Введите логин");
        String login = loginDialog.showAndWait().orElse("").trim();
        if (login.isBlank()) return;

        TextInputDialog passDialog = new TextInputDialog();
        passDialog.setTitle("Новый сотрудник");
        passDialog.setHeaderText("Введите пароль");
        String password = passDialog.showAndWait().orElse("");
        if (password.isBlank()) return;

        TextInputDialog nameDialog = new TextInputDialog();
        nameDialog.setTitle("Новый сотрудник");
        nameDialog.setHeaderText("Введите ФИО");
        String fullName = nameDialog.showAndWait().orElse("").trim();
        if (fullName.isBlank()) return;

        ChoiceDialog<Role> roleDialog = new ChoiceDialog<>(Role.USER, Role.values());
        roleDialog.setTitle("Новый сотрудник");
        roleDialog.setHeaderText("Выберите роль");
        Role role = roleDialog.showAndWait().orElse(null);
        if (role == null) return;

        String payload = encodePayload(login, password, fullName, role.name());
        new Thread(() -> {
            String response = networkClient.sendCommand("add_user " + payload);
            Platform.runLater(() -> {
                if (response != null && response.startsWith("OK:")) {
                    onSaved.run();
                } else {
                    showAlert("Ошибка добавления: " + response);
                }
            });
        }).start();
    }

    private void showEditUserDialog(String[] selected, Runnable onSaved) {
        int userId = Integer.parseInt(selected[0]);
        TextInputDialog loginDialog = new TextInputDialog(selected[1]);
        loginDialog.setTitle("Редактирование сотрудника");
        loginDialog.setHeaderText("Изменить логин");
        String login = loginDialog.showAndWait().orElse("").trim();
        if (login.isBlank()) return;

        TextInputDialog nameDialog = new TextInputDialog(selected[2]);
        nameDialog.setTitle("Редактирование сотрудника");
        nameDialog.setHeaderText("Изменить ФИО");
        String fullName = nameDialog.showAndWait().orElse("").trim();
        if (fullName.isBlank()) return;

        ChoiceDialog<Role> roleDialog = new ChoiceDialog<>(Role.valueOf(selected[3]), Role.values());
        roleDialog.setTitle("Редактирование сотрудника");
        roleDialog.setHeaderText("Изменить роль");
        Role role = roleDialog.showAndWait().orElse(null);
        if (role == null) return;

        TextInputDialog passDialog = new TextInputDialog();
        passDialog.setTitle("Редактирование сотрудника");
        passDialog.setHeaderText("Новый пароль (можно оставить пустым)");
        String password = passDialog.showAndWait().orElse("");

        String payload = encodePayload(String.valueOf(userId), login, fullName, role.name(), password);
        new Thread(() -> {
            String response = networkClient.sendCommand("change_role " + payload);
            Platform.runLater(() -> {
                if (response != null && response.startsWith("OK:")) {
                    onSaved.run();
                } else {
                    showAlert("Ошибка редактирования: " + response);
                }
            });
        }).start();
    }

    private String encodePayload(String... fields) {
        String joined = String.join("|", fields);
        return Base64.getEncoder().encodeToString(joined.getBytes(StandardCharsets.UTF_8));
    }

    @FXML
    private void handleAddCredit() {
        if (currentUser != null && currentUser.getRole() == Role.SUPER_ADMIN) {
            showAlert("Супер-администратор не оформляет кредиты");
            return;
        }
        Client selected = clientsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Выберите клиента");
            return;
        }
        if (selected.getBirthYear() == null) {
            showAlert("Укажите год рождения клиента в карточке (редактирование клиента).");
            return;
        }
        int ageYears = LocalDate.now().getYear() - selected.getBirthYear();
        if (ageYears < 18) {
            showAlert("Клиент несовершеннолетний, оформление кредита невозможно.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/add-credit.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle("Оформить кредит");
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(clientsTable.getScene().getWindow());
            AddCreditController ctrl = loader.getController();
            ctrl.setClient(selected);
            ctrl.setNetworkClient(networkClient);
            ctrl.setCurrentUserId(currentUser.getId());
            stage.showAndWait();
            if (ctrl.isSaved()) {
                loadCreditsForClient(selected.getId());
                updateStatus("Кредит оформлен, ID: " + ctrl.getCreatedCreditId());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleShowPayments() {
        Credit selected = creditsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Выберите кредит");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/payments-view.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle("График платежей");
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(clientsTable.getScene().getWindow());
            PaymentsController ctrl = loader.getController();
            ctrl.setCredit(selected);
            ctrl.setNetworkClient(networkClient);
            ctrl.loadPayments();
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}