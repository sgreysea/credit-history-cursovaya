package com.credithistory.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.Socket;
import java.time.LocalDate;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class ClientHandler implements Runnable {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private UserDAO userDAO;
    private ClientDAO clientDAO;
    private CreditDAO creditDAO;
    private PaymentDAO paymentDAO;
    private User currentUser;
    private final ScoreCalculator scoreCalculator;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.userDAO = new UserDAO();
        this.clientDAO = new ClientDAO();
        this.creditDAO = new CreditDAO();
        this.paymentDAO = new PaymentDAO();
        this.scoreCalculator = new ScoreCalculator();
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
        String[] commandParts = command.split(" ", 2);
        String action = parts[0];
        try {
            switch (action) {
                case "login": return handleLogin(parts);
                case "register": return handleRegister(parts);
                case "logout": return handleLogout();
                case "get_clients": return handleGetClients();
                case "search_clients": return handleSearchClients(parts);
                case "add_client": return handleAddClient(commandParts);
                case "update_client": return handleUpdateClient(commandParts);
                case "delete_client": return handleDeleteClient(parts);
                case "get_credits": return handleGetCredits(parts);
                case "add_credit": return handleAddCredit(parts);
                case "close_credit": return handleCloseCredit(parts);
                case "get_credit_info": return handleGetCreditInfo(parts);
                case "skip_payment": return handleSkipPayment(parts);
                case "get_payments": return handleGetPayments(parts);
                case "mark_payment": return handleMarkPayment(parts);
                case "get_statistics": return handleGetStatistics();
                case "calculate_rating": return handleCalculateRating(parts);
                case "get_client_bio": return handleGetClientBio(parts);
                case "get_users": return handleGetUsers();
                case "add_user": return handleAddUser(commandParts);
                case "delete_user": return handleDeleteUser(parts);
                case "change_role": return handleChangeRole(commandParts);
                case "get_employee_stats": return handleGetEmployeeStats();
                default: return "ERROR: Неизвестная команда";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR: " + e.getMessage();
        }
    }

    private String handleGetCreditInfo(String[] parts) {
        if (parts.length < 2) return "ERROR: Укажите ID кредита";
        Credit credit = creditDAO.findById(Integer.parseInt(parts[1]));
        if (credit == null) return "ERROR: Кредит не найден";
        return String.format("OK:%s|%d|%s", credit.getAmount(), credit.getTermMonths(), credit.getInterestRate());
    }

    private String handleGetEmployeeStats() {
        if (currentUser == null) return "ERROR: Не авторизован";
        if (currentUser.getRole() != Role.SUPER_ADMIN) return "ERROR: Только супер-админ может смотреть статистику";

        Map<Integer, Integer> creditsByEmployee = new HashMap<>();
        for (Credit credit : creditDAO.getAllCredits()) {
            creditsByEmployee.merge(credit.getUserId(), 1, Integer::sum);
        }

        StringBuilder sb = new StringBuilder("OK:");
        boolean first = true;
        for (User user : userDAO.getAllUsers()) {
            if (!user.isActive()) continue;
            int clientsRegistered = clientDAO.countClientsRegisteredByUser(user.getId());
            int creditsIssued = creditsByEmployee.getOrDefault(user.getId(), 0);
            if (!first) sb.append(";");
            sb.append(user.getId()).append("|").append(user.getFullName()).append("|")
                    .append(clientsRegistered).append("|").append(creditsIssued);
            first = false;
        }
        return sb.toString();
    }

    private String handleSkipPayment(String[] parts) {
        if (currentUser == null) return "ERROR: Не авторизован";
        if (parts.length < 2) return "ERROR: Укажите ID платежа";
        boolean skipped = paymentDAO.skipPayment(Integer.parseInt(parts[1]));
        return skipped ? "OK: Платёж отмечен как пропущенный, начислен штраф" : "ERROR: Не удалось";
    }

    private String handleGetClientBio(String[] parts) {
        if (currentUser == null) return "ERROR: Не авторизован";
        if (parts.length < 2) return "ERROR: Укажите ID клиента";
        ClientStatistics stats = clientDAO.getClientStatistics(Integer.parseInt(parts[1]));
        if (stats == null) return "ERROR: Клиент не найден";
        int score = scoreCalculator.calculateScore(stats.getClientId());
        stats.setRatingScore(score);
        stats.setRatingLetter(scoreCalculator.getRatingLetter(score));
        stats.setRatingColor(scoreCalculator.getRatingColor(score));

        List<Credit> credits = creditDAO.getCreditsByClientId(stats.getClientId());
        int crActive = 0, crOverdue = 0, crClosedNormal = 0, crClosedEarly = 0;
        for (Credit credit : credits) {
            switch (credit.getStatus()) {
                case ACTIVE -> crActive++;
                case OVERDUE -> crOverdue++;
                case CLOSED -> {
                    if (scoreCalculator.isClosedEarlyPaidOff(credit)) crClosedEarly++;
                    else crClosedNormal++;
                }
            }
        }

        return String.format("OK:%s|%s|%d|%d|%d|%d|%d|%d|%d|%s|%s|%d|%d|%d|%d",
                stats.getFullName(), stats.getCreatedAt(), stats.getTotalCredits(),
                stats.getActiveCredits(), stats.getClosedCredits(), stats.getTotalPaid(),
                stats.getPaidOnTime(), stats.getEarlyPayments(), stats.getTotalOverdue(),
                stats.getRatingLetter(), stats.getRatingColor(),
                crActive, crOverdue, crClosedNormal, crClosedEarly);
    }

    private String handleLogin(String[] parts) {
        if (parts.length < 3) return "ERROR: Неверный формат";
        User user = userDAO.findByLogin(parts[1]);
        if (user != null && user.getPassword().equals(parts[2])) {
            currentUser = user;
            return "OK:" + user.getId() + ":" + user.getRole().name() + ":" + user.getFullName();
        }
        return "ERROR: Неверный логин или пароль";
    }

    private String handleRegister(String[] parts) {
        if (parts.length < 3) return "ERROR: Неверный формат";
        if (userDAO.findByLogin(parts[1]) != null) return "ERROR: Логин уже занят";
        return userDAO.createUser(parts[1], parts[2], parts.length > 3 ? parts[3] : parts[1]) ? "OK: Успешно" : "ERROR: Ошибка";
    }

    private String handleLogout() { currentUser = null; return "OK: Выход выполнен"; }

    private String handleGetClients() {
        if (currentUser == null) return "ERROR: Не авторизован";
        List<Client> clients = clientDAO.getAllClients();
        Map<Integer, String> names = new HashMap<>();
        for (User u : userDAO.getAllUsers()) {
            String[] np = u.getFullName().split(" ");
            String sn = np[0];
            if (np.length > 1) sn += " " + np[1].charAt(0) + ".";
            if (np.length > 2) sn += np[2].charAt(0) + ".";
            names.put(u.getId(), sn);
        }
        StringBuilder sb = new StringBuilder("OK:");
        boolean first = true;
        for (Client c : clients) {
            if (!first) sb.append(";");
            first = false;
            int creditCnt = creditDAO.countCreditsByClientId(c.getId());
            String birthYearField = c.getBirthYear() != null ? String.valueOf(c.getBirthYear()) : "";
            String email = c.getEmail() != null ? c.getEmail() : "";
            String address = c.getAddress() != null ? c.getAddress() : "";
            sb.append(c.getId()).append("|").append(c.getFullName()).append("|")
                    .append(c.getPassport()).append("|").append(c.getPhone() == null ? "-" : c.getPhone()).append("|")
                    .append(c.getRegisteredBy()).append("|").append(names.getOrDefault(c.getRegisteredBy(), "?")).append("|");
            int score = scoreCalculator.calculateScore(c.getId());
            sb.append(score).append("|").append(scoreCalculator.getRatingLetter(score)).append("|")
                    .append(birthYearField).append("|").append(creditCnt).append("|")
                    .append(email).append("|").append(address);
        }
        return sb.toString();
    }

    private String handleSearchClients(String[] parts) {
        if (currentUser == null) return "ERROR: Не авторизован";
        if (parts.length < 2) return "ERROR: Укажите поисковый запрос";

        String query = parts[1].toLowerCase();
        List<Client> allClients = clientDAO.getAllClients();
        Map<Integer, String> names = new HashMap<>();
        for (User u : userDAO.getAllUsers()) {
            String[] np = u.getFullName().split(" ");
            String sn = np[0];
            if (np.length > 1) sn += " " + np[1].charAt(0) + ".";
            if (np.length > 2) sn += np[2].charAt(0) + ".";
            names.put(u.getId(), sn);
        }

        List<Client> filtered = new ArrayList<>();
        for (Client c : allClients) {
            if (c.getFullName().toLowerCase().contains(query) ||
                    c.getPassport().toLowerCase().contains(query) ||
                    (c.getPhone() != null && c.getPhone().contains(query))) {
                filtered.add(c);
            }
        }

        StringBuilder sb = new StringBuilder("OK:");
        boolean first = true;
        for (Client c : filtered) {
            if (!first) sb.append(";");
            first = false;
            int creditCnt = creditDAO.countCreditsByClientId(c.getId());
            String birthYearField = c.getBirthYear() != null ? String.valueOf(c.getBirthYear()) : "";
            sb.append(c.getId()).append("|").append(c.getFullName()).append("|")
                    .append(c.getPassport()).append("|").append(c.getPhone() == null ? "-" : c.getPhone()).append("|")
                    .append(c.getRegisteredBy()).append("|").append(names.getOrDefault(c.getRegisteredBy(), "?")).append("|");
            int score = scoreCalculator.calculateScore(c.getId());
            sb.append(score).append("|").append(scoreCalculator.getRatingLetter(score)).append("|")
                    .append(birthYearField).append("|").append(creditCnt);
        }
        return sb.toString();
    }

    private String handleAddClient(String[] commandParts) {
        if (currentUser == null) return "ERROR: Не авторизован";
        if (commandParts.length < 2) return "ERROR: Неверный формат";
        String[] d = decodePayload(commandParts[1]);
        if (d.length < 3) return "ERROR: Неверный формат";
        String email = d.length > 3 ? d[3] : "";
        String address = d.length > 4 ? d[4] : "";
        String birthYearStr = d.length > 5 ? d[5] : "";
        if (!isValidEmail(email)) return "ERROR: Некорректный email";
        Integer birthYear = parseBirthYear(birthYearStr);
        Client c = new Client(d[0], d[1], d[2], currentUser.getId());
        c.setEmail(email);
        c.setAddress(address);
        c.setBirthYear(birthYear);
        return clientDAO.createClient(c) ? "OK:" + c.getId() : "ERROR: Ошибка создания клиента";
    }

    private String handleUpdateClient(String[] commandParts) {
        if (currentUser == null) return "ERROR: Не авторизован";
        if (commandParts.length < 2) return "ERROR: Неверный формат";
        String[] d = decodePayload(commandParts[1]);
        if (d.length < 4) return "ERROR: Неверный формат";
        Client c = clientDAO.findById(Integer.parseInt(d[0]));
        if (c == null) return "ERROR: Клиент не найден";
        String email = d.length > 4 ? d[4] : "";
        String address = d.length > 5 ? d[5] : "";
        String birthYearStr = d.length > 6 ? d[6] : "";
        if (!isValidEmail(email)) return "ERROR: Некорректный email";
        c.setFullName(d[1]); c.setPassport(d[2]); c.setPhone(d[3]);
        c.setEmail(email); c.setAddress(address);
        c.setBirthYear(parseBirthYear(birthYearStr));
        return clientDAO.updateClient(c) ? "OK: Обновлён" : "ERROR: Ошибка обновления";
    }

    private Integer parseBirthYear(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            int y = Integer.parseInt(raw.trim());
            if (y < 1900 || y > LocalDate.now().getYear()) return null;
            return y;
        } catch (NumberFormatException e) { return null; }
    }

    private boolean isAdult(Integer birthYear) {
        return birthYear != null && (LocalDate.now().getYear() - birthYear) >= 18;
    }

    private String handleDeleteClient(String[] parts) {
        if (currentUser == null) return "ERROR: Не авторизован";
        if (currentUser.getRole() != Role.ADMIN && currentUser.getRole() != Role.SUPER_ADMIN) return "ERROR: Недостаточно прав";
        return clientDAO.deleteClient(Integer.parseInt(parts[1])) ? "OK: Удалён" : "ERROR: Ошибка удаления";
    }

    private String handleGetCredits(String[] parts) {
        if (currentUser == null) return "ERROR: Не авторизован";
        List<Credit> credits = parts.length > 1 ? creditDAO.getCreditsByClientId(Integer.parseInt(parts[1])) : creditDAO.getAllCredits();
        Map<Integer, String> names = new HashMap<>();
        for (User u : userDAO.getAllUsers()) {
            String[] np = u.getFullName().split(" ");
            String sn = np[0];
            if (np.length > 1) sn += " " + np[1].charAt(0) + ".";
            if (np.length > 2) sn += np[2].charAt(0) + ".";
            names.put(u.getId(), sn);
        }
        StringBuilder sb = new StringBuilder("OK:");
        for (Credit c : credits) {
            sb.append(c.getId()).append("|").append(c.getClientId()).append("|")
                    .append(c.getAmount()).append("|").append(c.getTermMonths()).append("|")
                    .append(c.getInterestRate()).append("|").append(c.getIssueDate()).append("|")
                    .append(c.getStatus()).append("|").append(c.getUserId()).append("|")
                    .append(names.getOrDefault(c.getUserId(), "?"));
            sb.append(";");
        }
        return sb.toString();
    }

    private String handleAddCredit(String[] parts) {
        if (currentUser == null) return "ERROR: Не авторизован";
        if (currentUser.getRole() == Role.SUPER_ADMIN) return "ERROR: Супер-админ не может добавлять кредиты";
        if (parts.length < 6) return "ERROR: Неверный формат";
        int clientId = Integer.parseInt(parts[1]);
        Client clientRow = clientDAO.findById(clientId);
        if (clientRow == null) return "ERROR: Клиент не найден";
        if (clientRow.getBirthYear() == null) return "ERROR: Не указан год рождения клиента";
        if (!isAdult(clientRow.getBirthYear())) return "ERROR: Клиент несовершеннолетний";
        int score = scoreCalculator.calculateScore(clientId);
        if (scoreCalculator.getRatingLetter(score).equals("E")) return "ERROR: Кредитный рейтинг слишком низкий (E)";
        Credit c = new Credit(clientId, currentUser.getId(), new BigDecimal(parts[2]),
                Integer.parseInt(parts[3]), new BigDecimal(parts[4]), LocalDate.parse(parts[5]));
        if (creditDAO.createCredit(c)) {
            paymentDAO.generatePaymentSchedule(c.getId(), c.getMonthlyPayment(), c.getIssueDate(), c.getTermMonths());
            return "OK:" + c.getId();
        }
        return "ERROR: Ошибка оформления кредита";
    }

    private String handleCloseCredit(String[] parts) {
        if (currentUser == null) return "ERROR: Не авторизован";
        int creditId = Integer.parseInt(parts[1]);
        List<Payment> payments = paymentDAO.getPaymentsByCreditId(creditId);
        boolean allDone = payments.stream().allMatch(p -> p.getStatus() != PaymentStatus.PENDING);
        if (!allDone) return "ERROR: Есть неоплаченные платежи";
        // Проверяем, вся ли сумма кредита выплачена
        Credit credit = creditDAO.findById(creditId);
        if (credit == null) return "ERROR: Кредит не найден";
        BigDecimal totalPaid = BigDecimal.ZERO;
        for (Payment p : payments) {
            if (p.getStatus() == PaymentStatus.PAID && p.getActualAmount() != null) {
                totalPaid = totalPaid.add(p.getActualAmount());
            }
        }
        if (totalPaid.compareTo(credit.getAmount()) < 0) {
            BigDecimal debt = credit.getAmount().subtract(totalPaid);
            // Добавляем дополнительный платёж с повышенной суммой (пеня 10%)
            BigDecimal penalty = debt.multiply(new BigDecimal("1.10"));
            paymentDAO.addExtraPayment(creditId, LocalDate.now().plusMonths(1), penalty);
            return "ERROR: Недостаточно выплат. Добавлен дополнительный платёж на " + penalty + " BYN";
        }
        return creditDAO.closeCredit(creditId) ? "OK: Закрыт" : "ERROR: Ошибка закрытия";
    }

    private String handleGetPayments(String[] parts) {
        if (currentUser == null) return "ERROR: Не авторизован";
        paymentDAO.updateOverduePayments();
        List<Payment> list = paymentDAO.getPaymentsByCreditId(Integer.parseInt(parts[1]));
        StringBuilder sb = new StringBuilder("OK:");
        for (Payment p : list) {
            sb.append(p.getId()).append("|").append(p.getPlannedDate()).append("|")
                    .append(p.getPlannedAmount()).append("|").append(p.getStatus()).append("|")
                    .append(p.getActualDate() != null ? p.getActualDate() : "-").append("|")
                    .append(p.getActualAmount() != null ? p.getActualAmount() : "0");
            sb.append(";");
        }
        return sb.toString();
    }

    private String handleMarkPayment(String[] parts) {
        if (currentUser == null) return "ERROR: Не авторизован";
        int payId = Integer.parseInt(parts[1]);
        BigDecimal amount = new BigDecimal(parts[2]);
        Payment pay = paymentDAO.findById(payId);
        if (pay == null) return "ERROR: Не найден";
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return "ERROR: Сумма платежа должна быть положительной";
        }

        List<Payment> all = paymentDAO.getPaymentsByCreditId(pay.getCreditId());
        for (Payment p : all) {
            if (p.getId() < payId && p.getStatus() == PaymentStatus.PENDING)
                return "ERROR: Оплатите сначала платёж #" + p.getId();
        }
        boolean paid = paymentDAO.markAsPaid(payId, amount);
        if (!paid) return "ERROR: Не удалось провести оплату";
        updateCreditStatus(pay.getCreditId());
        return amount.compareTo(pay.getPlannedAmount()) > 0
                ? "OK: Оплачено с переплатой, будущие платежи уменьшены"
                : "OK: Оплачено";
    }

    private void updateCreditStatus(int creditId) {
        List<Payment> pp = paymentDAO.getPaymentsByCreditId(creditId);
        if (pp.stream().allMatch(p -> p.getStatus() != PaymentStatus.PENDING)) creditDAO.closeCredit(creditId);
    }

    private String handleGetStatistics() {
        if (currentUser == null || currentUser.getRole() == Role.USER) return "ERROR: Недостаточно прав";
        CreditDAO.CreditStatistics s = creditDAO.getStatistics();
        return "OK:" + s.getTotalCredits() + "|" + s.getTotalAmount() + "|" + s.getActiveCount() + "|" + s.getClosedCount() + "|" + s.getOverdueCount();
    }

    private String handleCalculateRating(String[] parts) {
        if (currentUser == null) return "ERROR: Не авторизован";
        int score = scoreCalculator.calculateScore(Integer.parseInt(parts[1]));
        return "OK:" + score + "|" + scoreCalculator.getRatingLetter(score) + "|" + scoreCalculator.getScoreCategory(score);
    }

    private String handleGetUsers() {
        if (currentUser == null || currentUser.getRole() != Role.SUPER_ADMIN) return "ERROR: Только супер-админ";
        StringBuilder sb = new StringBuilder("OK:");
        boolean first = true;
        for (User user : userDAO.getAllUsers()) {
            if (!first) sb.append(";");
            sb.append(user.getId()).append("|").append(user.getLogin()).append("|")
                    .append(user.getFullName()).append("|").append(user.getRole().name()).append("|")
                    .append(user.isActive());
            first = false;
        }
        return sb.toString();
    }

    private String handleAddUser(String[] commandParts) {
        if (currentUser == null || currentUser.getRole() != Role.SUPER_ADMIN) return "ERROR: Только супер-админ";
        if (commandParts.length < 2) return "ERROR: Неверный формат";
        String[] d = decodePayload(commandParts[1]);
        if (d.length < 4) return "ERROR: Неверный формат";
        String login = d[0].trim(), password = d[1], fullName = d[2].trim();
        Role role = Role.valueOf(d[3].trim());
        if (login.isBlank() || password.isBlank() || fullName.isBlank()) return "ERROR: Пустые поля недопустимы";
        if (userDAO.existsByLogin(login)) return "ERROR: Логин уже занят";
        return userDAO.createUserWithRole(login, password, fullName, role) ? "OK: Создан" : "ERROR: Ошибка создания";
    }

    private String handleDeleteUser(String[] parts) {
        if (currentUser == null || currentUser.getRole() != Role.SUPER_ADMIN) return "ERROR: Только супер-админ";
        if (parts.length < 2) return "ERROR: Укажите ID пользователя";
        int userId = Integer.parseInt(parts[1]);
        if (currentUser.getId() == userId) return "ERROR: Нельзя удалить текущего пользователя";
        User target = userDAO.findById(userId);
        if (target == null) return "ERROR: Пользователь не найден";
        if (target.getRole() == Role.SUPER_ADMIN && userDAO.countByRole(Role.SUPER_ADMIN) <= 1)
            return "ERROR: Должен остаться минимум один SUPER_ADMIN";
        return userDAO.deactivateUser(userId) ? "OK: Удалён" : "ERROR: Ошибка удаления";
    }

    private String handleChangeRole(String[] commandParts) {
        if (currentUser == null || currentUser.getRole() != Role.SUPER_ADMIN) return "ERROR: Только супер-админ";
        if (commandParts.length < 2) return "ERROR: Неверный формат";
        String[] d = decodePayload(commandParts[1]);
        if (d.length < 4) return "ERROR: Неверный формат";
        int userId = Integer.parseInt(d[0]);
        String login = d[1].trim(), fullName = d[2].trim();
        Role newRole = Role.valueOf(d[3].trim());
        String password = d.length > 4 ? d[4] : "";

        User target = userDAO.findById(userId);
        if (target == null) return "ERROR: Пользователь не найден";
        if (login.isBlank() || fullName.isBlank()) return "ERROR: Пустые поля недопустимы";

        User byLogin = userDAO.findByLogin(login);
        if (byLogin != null && byLogin.getId() != userId) return "ERROR: Логин уже занят";

        if (target.getRole() == Role.SUPER_ADMIN && newRole != Role.SUPER_ADMIN && userDAO.countByRole(Role.SUPER_ADMIN) <= 1)
            return "ERROR: Должен остаться минимум один SUPER_ADMIN";
        if (currentUser.getId() == userId && newRole != Role.SUPER_ADMIN && userDAO.countByRole(Role.SUPER_ADMIN) <= 1)
            return "ERROR: Нельзя разжаловать последнего SUPER_ADMIN";

        return userDAO.updateUserProfileAndRole(userId, login, fullName, newRole, password) ? "OK: Обновлён" : "ERROR: Ошибка обновления";
    }

    private String[] decodePayload(String encodedPayload) {
        return new String(Base64.getDecoder().decode(encodedPayload), StandardCharsets.UTF_8).split("\\|", -1);
    }

    private boolean isValidEmail(String email) {
        return email == null || email.isBlank() || EMAIL_PATTERN.matcher(email).matches();
    }

    private void closeConnection() {
        try { if (in != null) in.close(); if (out != null) out.close(); if (socket != null) socket.close(); }
        catch (IOException ignored) {}
    }
}