package com.credithistory.database;

import com.credithistory.model.Credit;
import com.credithistory.model.CreditStatus;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CreditDAO {

    // Получить все кредиты
    public List<Credit> getAllCredits() {
        List<Credit> credits = new ArrayList<>();
        String sql = "SELECT * FROM credits ORDER BY issue_date DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                credits.add(mapResultSetToCredit(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return credits;
    }

    // Получить кредиты конкретного клиента
    public List<Credit> getCreditsByClientId(int clientId) {
        List<Credit> credits = new ArrayList<>();
        String sql = "SELECT * FROM credits WHERE client_id = ? ORDER BY issue_date DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, clientId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                credits.add(mapResultSetToCredit(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return credits;
    }

    // Получить кредит по ID
    public Credit findById(int id) {
        String sql = "SELECT * FROM credits WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToCredit(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Создать новый кредит
    public boolean createCredit(Credit credit) {
        String sql = "INSERT INTO credits (client_id, user_id, amount, term_months, " +
                "interest_rate, issue_date, status) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, credit.getClientId());
            stmt.setInt(2, credit.getUserId());
            stmt.setBigDecimal(3, credit.getAmount());
            stmt.setInt(4, credit.getTermMonths());
            stmt.setBigDecimal(5, credit.getInterestRate());
            stmt.setDate(6, Date.valueOf(credit.getIssueDate()));
            stmt.setString(7, credit.getStatus().name());

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    credit.setId(generatedKeys.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Обновить статус кредита
    public boolean updateStatus(int creditId, CreditStatus status) {
        String sql = "UPDATE credits SET status = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status.name());
            stmt.setInt(2, creditId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Закрыть кредит (погашен)
    public boolean closeCredit(int creditId) {
        return updateStatus(creditId, CreditStatus.CLOSED);
    }

    // Пометить как просроченный
    public boolean markAsOverdue(int creditId) {
        return updateStatus(creditId, CreditStatus.OVERDUE);
    }

    // Получить активные кредиты
    public List<Credit> getActiveCredits() {
        List<Credit> credits = new ArrayList<>();
        String sql = "SELECT * FROM credits WHERE status = 'ACTIVE' ORDER BY issue_date DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                credits.add(mapResultSetToCredit(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return credits;
    }

    // Получить просроченные кредиты
    public List<Credit> getOverdueCredits() {
        List<Credit> credits = new ArrayList<>();
        String sql = "SELECT * FROM credits WHERE status = 'OVERDUE' ORDER BY issue_date DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                credits.add(mapResultSetToCredit(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return credits;
    }

    // Получить статистику по кредитам
    public CreditStatistics getStatistics() {
        String sql = "SELECT " +
                "COUNT(*) as total_credits, " +
                "SUM(amount) as total_amount, " +
                "SUM(CASE WHEN status = 'ACTIVE' THEN 1 ELSE 0 END) as active_count, " +
                "SUM(CASE WHEN status = 'CLOSED' THEN 1 ELSE 0 END) as closed_count, " +
                "SUM(CASE WHEN status = 'OVERDUE' THEN 1 ELSE 0 END) as overdue_count " +
                "FROM credits";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return new CreditStatistics(
                        rs.getInt("total_credits"),
                        rs.getBigDecimal("total_amount"),
                        rs.getInt("active_count"),
                        rs.getInt("closed_count"),
                        rs.getInt("overdue_count")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new CreditStatistics(0, java.math.BigDecimal.ZERO, 0, 0, 0);
    }

    // Вспомогательный метод для маппинга ResultSet -> Credit
    private Credit mapResultSetToCredit(ResultSet rs) throws SQLException {
        Credit credit = new Credit();
        credit.setId(rs.getInt("id"));
        credit.setClientId(rs.getInt("client_id"));
        credit.setUserId(rs.getInt("user_id"));
        credit.setAmount(rs.getBigDecimal("amount"));
        credit.setTermMonths(rs.getInt("term_months"));
        credit.setInterestRate(rs.getBigDecimal("interest_rate"));
        credit.setIssueDate(rs.getDate("issue_date").toLocalDate());
        credit.setStatus(CreditStatus.valueOf(rs.getString("status")));
        credit.setCreatedAt(rs.getTimestamp("created_at"));
        return credit;
    }

    // Внутренний класс для статистики
    public static class CreditStatistics {
        private final int totalCredits;
        private final java.math.BigDecimal totalAmount;
        private final int activeCount;
        private final int closedCount;
        private final int overdueCount;

        public CreditStatistics(int totalCredits, java.math.BigDecimal totalAmount,
                                int activeCount, int closedCount, int overdueCount) {
            this.totalCredits = totalCredits;
            this.totalAmount = totalAmount;
            this.activeCount = activeCount;
            this.closedCount = closedCount;
            this.overdueCount = overdueCount;
        }

        public int getTotalCredits() { return totalCredits; }
        public java.math.BigDecimal getTotalAmount() { return totalAmount; }
        public int getActiveCount() { return activeCount; }
        public int getClosedCount() { return closedCount; }
        public int getOverdueCount() { return overdueCount; }
    }
}