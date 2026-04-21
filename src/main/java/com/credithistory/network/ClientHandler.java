package com.credithistory.network;

import com.credithistory.database.*;
import com.credithistory.model.*;

import java.io.*;
import java.net.Socket;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ClientHandler implements Runnable {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    // DAO объекты
    private UserDAO userDAO;
    private ClientDAO clientDAO;
    private CreditDAO creditDAO;
    private PaymentDAO paymentDAO;

    // Текущий авторизованный пользователь
    private User currentUser;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.userDAO = new UserDAO();
        this.clientDAO = new ClientDAO();
        this.creditDAO = new CreditDAO();
        this.paymentDAO = new PaymentDAO();
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String command;
            while ((command = in.readLine()) != null) {
                System.out.println("Получена команда: " + command);
                String response = processCommand(command);
                out.println(response);
                System.out.println("Отправлен ответ: " + response);
            }
        } catch (IOException e) {
            System.err.println("Ошибка в ClientHandler: " + e.getMessage());
        } finally {
            closeConnection();
        }
    }

    private String processCommand(String command) {
        String[] parts = command.split(" ");
        String action = parts[0];

        try {
            switch (action) {
                // АВТОРИЗАЦИЯ И РЕГИСТРАЦИЯ
                case "login":
                    return handleLogin(parts);
                case "register":
                    return handleRegister(parts);
                case "logout":
                    return handleLogout();

                // РАБОТА С КЛИЕНТАМИ
                case "get_clients":
                    return handleGetClients();
                case "search_clients":
                    return handleSearchClients(parts);
                case "add_client":
                    return handleAddClient(parts);
                case "update_client":
                    return handleUpdateClient(parts);
                case "delete_client":
                    return handleDeleteClient(parts);

                // РАБОТА С КРЕДИТАМИ
                case "get_credits":
                    return handleGetCredits(parts);
                case "add_credit":
                    return handleAddCredit(parts);
                case "close_credit":
                    return handleCloseCredit(parts);

                // РАБОТА С ПЛАТЕЖАМИ
                case "get_payments":
                    return handleGetPayments(parts);
                case "mark_payment":
                    return handleMarkPayment(parts);

                // СТАТИСТИКА И РЕЙТИНГ
                case "get_statistics":
                    return handleGetStatistics();
                case "calculate_rating":
                    return handleCalculateRating(parts);

                // АДМИНИСТРИРОВАНИЕ
                case "get_users":
                    return handleGetUsers();
                case "add_user":
                    return handleAddUser(parts);
                case "delete_user":
                    return handleDeleteUser(parts);
                case "change_role":
                    return handleChangeRole(parts);
                case "get_credit_info":
                    return handleGetCreditInfo(parts);
                case "skip_payment":
                    return handleSkipPayment(parts);
                default:
                    return "ERROR:Неизвестная команда";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR:" + e.getMessage();
        }
    }
    private String handleGetCreditInfo(String[] parts) {
        if (parts.length < 2) {
            return "ERROR:Укажите ID кредита";
        }

        int creditId = Integer.parseInt(parts[1]);
        Credit credit = creditDAO.findById(creditId);

        if (credit == null) {
            return "ERROR:Кредит не найден";
        }

        return String.format("OK:%s|%d|%s",
                credit.getAmount().toString(),
                credit.getTermMonths(),
                credit.getInterestRate().toString());
    }

    private String handleSkipPayment(String[] parts) {
        if (currentUser == null) {
            return "ERROR:Не авторизован";
        }

        if (parts.length < 2) {
            return "ERROR:Укажите ID платежа";
        }

        int paymentId = Integer.parseInt(parts[1]);

        // Отмечаем платёж как просроченный
        boolean skipped = paymentDAO.skipPayment(paymentId);

        if (skipped) {
            return "OK:Платёж отмечен как пропущенный, начислен штраф";
        }

        return "ERROR:Не удалось обработать платёж";
    }

    // ==================== АВТОРИЗАЦИЯ ====================

    private String handleLogin(String[] parts) {
        if (parts.length < 3) {
            return "ERROR:Неверный формат";
        }

        String login = parts[1];
        String password = parts[2];

        System.out.println("Попытка входа: логин='" + login + "', пароль='" + password + "'");

        User user = userDAO.findByLogin(login);

        if (user != null) {
            System.out.println("Найден пользователь: " + user.getLogin() +
                    ", пароль в БД: '" + user.getPassword() + "'");

            if (user.getPassword().equals(password)) {
                currentUser = user;
                return "OK:" + user.getRole().name() + ":" + user.getFullName();
            } else {
                System.out.println("Пароль не совпадает!");
            }
        } else {
            System.out.println("Пользователь не найден!");
        }

        return "ERROR:Неверный логин или пароль";
    }

    private String handleRegister(String[] parts) {
        if (parts.length < 3) {
            return "ERROR:Неверный формат. Используйте: register <логин> <пароль>";
        }

        String login = parts[1];
        String password = parts[2];
        String fullName = login;

        if (parts.length >= 4) {
            fullName = parts[3];
        }

        // Проверяем, существует ли уже такой пользователь
        if (userDAO.findByLogin(login) != null) {
            return "ERROR:Пользователь с таким логином уже существует";
        }

        boolean created = userDAO.createUser(login, password, fullName);

        if (created) {
            return "OK:Регистрация успешна";
        }

        return "ERROR:Ошибка при регистрации";
    }

    private String handleLogout() {
        currentUser = null;
        return "OK:Выход выполнен";
    }

    // ==================== КЛИЕНТЫ ====================

    private String handleGetClients() {
        if (currentUser == null) {
            return "ERROR:Не авторизован";
        }

        List<Client> clients = clientDAO.getAllClients();
        System.out.println("Найдено клиентов: " + clients.size());

        if (clients.isEmpty()) {
            return "OK:";
        }

        StringBuilder sb = new StringBuilder("OK:");
        for (Client client : clients) {
            sb.append(client.getId()).append("|")
                    .append(client.getFullName()).append("|")
                    .append(client.getPassport()).append("|")
                    .append(client.getPhone() != null ? client.getPhone() : "-");
            sb.append(";");
        }

        return sb.toString();
    }

    private String handleSearchClients(String[] parts) {
        if (currentUser == null) {
            return "ERROR:Не авторизован";
        }

        if (parts.length < 2) {
            return "ERROR:Укажите поисковый запрос";
        }

        String query = parts[1];
        List<Client> clients = clientDAO.searchByName(query);
        StringBuilder sb = new StringBuilder("OK:");

        for (Client client : clients) {
            sb.append(client.getId()).append("|")
                    .append(client.getFullName()).append("|")
                    .append(client.getPassport()).append("|")
                    .append(client.getPhone() != null ? client.getPhone() : "-");
            sb.append(";");
        }

        return sb.toString();
    }

    private String handleAddClient(String[] parts) {
        if (currentUser == null) {
            return "ERROR:Не авторизован";
        }

        if (parts.length < 4) {
            return "ERROR:Неверный формат. Используйте: add_client <ФИО> <паспорт> <телефон>";
        }

        String fullName = parts[1];
        String passport = parts[2];
        String phone = parts[3];

        Client client = new Client(fullName, passport, phone, currentUser.getId());
        boolean created = clientDAO.createClient(client);

        if (created) {
            return "OK:" + client.getId();
        }

        return "ERROR:Не удалось добавить клиента";
    }

    private String handleUpdateClient(String[] parts) {
        if (currentUser == null) {
            return "ERROR:Не авторизован";
        }

        if (parts.length < 5) {
            return "ERROR:Неверный формат";
        }

        int id = Integer.parseInt(parts[1]);
        String fullName = parts[2];
        String passport = parts[3];
        String phone = parts[4];

        Client client = clientDAO.findById(id);
        if (client == null) {
            return "ERROR:Клиент не найден";
        }

        client.setFullName(fullName);
        client.setPassport(passport);
        client.setPhone(phone);

        boolean updated = clientDAO.updateClient(client);

        if (updated) {
            return "OK:Данные обновлены";
        }

        return "ERROR:Не удалось обновить данные";
    }

    private String handleDeleteClient(String[] parts) {
        if (currentUser == null) {
            return "ERROR:Не авторизован";
        }

        if (currentUser.getRole() != Role.ADMIN && currentUser.getRole() != Role.SUPER_ADMIN) {
            return "ERROR:Недостаточно прав";
        }

        if (parts.length < 2) {
            return "ERROR:Укажите ID клиента";
        }

        int id = Integer.parseInt(parts[1]);
        boolean deleted = clientDAO.deleteClient(id);

        if (deleted) {
            return "OK:Клиент удалён";
        }

        return "ERROR:Не удалось удалить клиента";
    }

    // ==================== КРЕДИТЫ ====================

    private String handleGetCredits(String[] parts) {
        if (currentUser == null) {
            return "ERROR:Не авторизован";
        }

        List<Credit> credits;

        if (parts.length >= 2) {
            int clientId = Integer.parseInt(parts[1]);
            credits = creditDAO.getCreditsByClientId(clientId);
        } else {
            credits = creditDAO.getAllCredits();
        }

        StringBuilder sb = new StringBuilder("OK:");

        for (Credit credit : credits) {
            sb.append(credit.getId()).append("|")
                    .append(credit.getClientId()).append("|")
                    .append(credit.getAmount()).append("|")
                    .append(credit.getTermMonths()).append("|")
                    .append(credit.getInterestRate()).append("|")
                    .append(credit.getIssueDate()).append("|")
                    .append(credit.getStatus().name());
            sb.append(";");
        }

        return sb.toString();
    }

    private String handleAddCredit(String[] parts) {
        if (currentUser == null) {
            return "ERROR:Не авторизован";
        }

        if (parts.length < 6) {
            return "ERROR:Неверный формат. add_credit <clientId> <сумма> <срок(мес)> <ставка> <дата>";
        }

        int clientId = Integer.parseInt(parts[1]);
        BigDecimal amount = new BigDecimal(parts[2]);
        int termMonths = Integer.parseInt(parts[3]);
        BigDecimal interestRate = new BigDecimal(parts[4]);
        LocalDate issueDate = LocalDate.parse(parts[5]);

        Credit credit = new Credit(clientId, currentUser.getId(), amount, termMonths, interestRate, issueDate);
        boolean created = creditDAO.createCredit(credit);

        if (created) {
            BigDecimal monthlyPayment = credit.getMonthlyPayment();
            paymentDAO.generatePaymentSchedule(credit.getId(), monthlyPayment, issueDate, termMonths);
            return "OK:" + credit.getId();
        }

        return "ERROR:Не удалось создать кредит";
    }

    private String handleCloseCredit(String[] parts) {
        if (currentUser == null) {
            return "ERROR:Не авторизован";
        }

        if (parts.length < 2) {
            return "ERROR:Укажите ID кредита";
        }

        int creditId = Integer.parseInt(parts[1]);
        boolean closed = creditDAO.closeCredit(creditId);

        if (closed) {
            return "OK:Кредит закрыт";
        }

        return "ERROR:Не удалось закрыть кредит";
    }

    // ==================== ПЛАТЕЖИ ====================

    private String handleGetPayments(String[] parts) {
        if (currentUser == null) {
            return "ERROR:Не авторизован";
        }

        if (parts.length < 2) {
            return "ERROR:Укажите ID кредита";
        }

        int creditId = Integer.parseInt(parts[1]);
        List<Payment> payments = paymentDAO.getPaymentsByCreditId(creditId);

        StringBuilder sb = new StringBuilder("OK:");

        for (Payment payment : payments) {
            sb.append(payment.getId()).append("|")
                    .append(payment.getPlannedDate()).append("|")
                    .append(payment.getPlannedAmount()).append("|")
                    .append(payment.getStatus().name()).append("|")
                    .append(payment.getActualDate() != null ? payment.getActualDate() : "-");
            sb.append(";");
        }

        return sb.toString();
    }

    private String handleMarkPayment(String[] parts) {
        if (currentUser == null) {
            return "ERROR:Не авторизован";
        }

        if (parts.length < 3) {
            return "ERROR:Укажите ID платежа и сумму";
        }

        int paymentId = Integer.parseInt(parts[1]);
        BigDecimal amount = new BigDecimal(parts[2]);

        Payment payment = paymentDAO.findById(paymentId);
        if (payment == null) {
            return "ERROR:Платёж не найден";
        }

        BigDecimal planned = payment.getPlannedAmount();
        String message = "";

        if (amount.compareTo(planned) < 0) {
            // Частичная оплата
            paymentDAO.markAsPaid(paymentId, amount);
            message = "Частичная оплата. Остаток: " + planned.subtract(amount) + " BYN";
        } else if (amount.compareTo(planned) > 0) {
            // Оплата с переплатой - идёт на досрочное погашение
            paymentDAO.markAsPaid(paymentId, planned);
            BigDecimal extra = amount.subtract(planned);
            paymentDAO.makeEarlyPayment(payment.getCreditId(), extra);
            message = "Оплачено с переплатой " + extra + " BYN. Выполнено досрочное погашение.";
        } else {
            // Точная оплата
            paymentDAO.markAsPaid(paymentId, amount);
            message = "Платёж оплачен полностью";
        }

        return "OK:" + message;
    }

    // ==================== СТАТИСТИКА ====================

    private String handleGetStatistics() {
        if (currentUser == null) {
            return "ERROR:Не авторизован";
        }

        if (currentUser.getRole() != Role.ADMIN && currentUser.getRole() != Role.SUPER_ADMIN) {
            return "ERROR:Недостаточно прав";
        }

        CreditDAO.CreditStatistics stats = creditDAO.getStatistics();

        return "OK:" + stats.getTotalCredits() + "|" +
                stats.getTotalAmount() + "|" +
                stats.getActiveCount() + "|" +
                stats.getClosedCount() + "|" +
                stats.getOverdueCount();
    }

    private String handleCalculateRating(String[] parts) {
        if (currentUser == null) {
            return "ERROR:Не авторизован";
        }

        if (parts.length < 2) {
            return "ERROR:Укажите ID клиента";
        }

        int clientId = Integer.parseInt(parts[1]);
        PaymentDAO.PaymentStatistics stats = paymentDAO.getPaymentStatisticsByClientId(clientId);

        // Простая формула расчёта рейтинга
        int score = 500;
        score += (int)(stats.getOnTimePercentage() * 3);
        score -= stats.getOverdueCount() * 50;

        score = Math.max(100, Math.min(850, score));

        return "OK:" + score;
    }

    // ==================== АДМИНИСТРИРОВАНИЕ ====================

    private String handleGetUsers() {
        if (currentUser == null) {
            return "ERROR:Не авторизован";
        }

        if (currentUser.getRole() != Role.ADMIN && currentUser.getRole() != Role.SUPER_ADMIN) {
            return "ERROR:Недостаточно прав";
        }

        // Нужно добавить метод getAllUsers() в UserDAO
        return "OK:Функция в разработке";
    }

    private String handleAddUser(String[] parts) {
        if (currentUser == null) {
            return "ERROR:Не авторизован";
        }

        if (currentUser.getRole() != Role.ADMIN && currentUser.getRole() != Role.SUPER_ADMIN) {
            return "ERROR:Недостаточно прав";
        }

        if (parts.length < 4) {
            return "ERROR:add_user <логин> <пароль> <ФИО>";
        }

        String login = parts[1];
        String password = parts[2];
        String fullName = parts[3];

        boolean created = userDAO.createUser(login, password, fullName);

        if (created) {
            return "OK:Пользователь создан";
        }

        return "ERROR:Не удалось создать пользователя";
    }

    private String handleDeleteUser(String[] parts) {
        if (currentUser == null) {
            return "ERROR:Не авторизован";
        }

        if (currentUser.getRole() != Role.SUPER_ADMIN) {
            return "ERROR:Только SUPER_ADMIN может удалять пользователей";
        }

        return "OK:Функция в разработке";
    }

    private String handleChangeRole(String[] parts) {
        if (currentUser == null) {
            return "ERROR:Не авторизован";
        }

        if (currentUser.getRole() != Role.SUPER_ADMIN) {
            return "ERROR:Только SUPER_ADMIN может менять роли";
        }

        return "OK:Функция в разработке";
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ====================

    private void closeConnection() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}