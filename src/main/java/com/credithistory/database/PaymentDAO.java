package com.credithistory.database;

import com.credithistory.model.Payment;
import com.credithistory.model.PaymentStatus;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PaymentDAO {

    // Получить все платежи по кредиту
    public List<Payment> getPaymentsByCreditId(int creditId) {
        List<Payment> payments = new ArrayList<>();
        String sql = "SELECT * FROM payments WHERE credit_id = ? ORDER BY planned_date";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, creditId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                payments.add(mapResultSetToPayment(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return payments;
    }

    // Получить платёж по ID
    public Payment findById(int id) {
        String sql = "SELECT * FROM payments WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToPayment(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Создать график платежей для кредита
    public boolean generatePaymentSchedule(int creditId, java.math.BigDecimal monthlyPayment,
                                           LocalDate startDate, int termMonths) {
        String sql = "INSERT INTO payments (credit_id, planned_date, planned_amount, status) " +
                "VALUES (?, ?, ?, 'PENDING')";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);

            for (int i = 1; i <= termMonths; i++) {
                stmt.setInt(1, creditId);
                stmt.setDate(2, Date.valueOf(startDate.plusMonths(i)));
                stmt.setBigDecimal(3, monthlyPayment);
                stmt.addBatch();
            }

            int[] results = stmt.executeBatch();
            conn.commit();

            return results.length == termMonths;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Отметить платёж как оплаченный
    public boolean markAsPaid(int paymentId, java.math.BigDecimal actualAmount) {
        String sql = "UPDATE payments SET status = 'PAID', actual_date = ?, actual_amount = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(LocalDate.now()));
            stmt.setBigDecimal(2, actualAmount);
            stmt.setInt(3, paymentId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Получить просроченные платежи
    public List<Payment> getOverduePayments() {
        List<Payment> payments = new ArrayList<>();
        String sql = "SELECT * FROM payments WHERE status = 'PENDING' AND planned_date < ? ORDER BY planned_date";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(LocalDate.now()));
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                payments.add(mapResultSetToPayment(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return payments;
    }

    // Автоматически пометить просроченные платежи
    public int markOverduePayments() {
        String sql = "UPDATE payments SET status = 'OVERDUE' " +
                "WHERE status = 'PENDING' AND planned_date < ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(LocalDate.now()));
            return stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    // Получить статистику по платежам клиента
    public PaymentStatistics getPaymentStatisticsByClientId(int clientId) {
        String sql = "SELECT " +
                "COUNT(*) as total_payments, " +
                "SUM(CASE WHEN p.status = 'PAID' THEN 1 ELSE 0 END) as paid_count, " +
                "SUM(CASE WHEN p.status = 'OVERDUE' THEN 1 ELSE 0 END) as overdue_count, " +
                "SUM(CASE WHEN p.status = 'PAID' AND p.actual_date <= p.planned_date THEN 1 ELSE 0 END) as ontime_count " +
                "FROM payments p " +
                "JOIN credits c ON p.credit_id = c.id " +
                "WHERE c.client_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, clientId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new PaymentStatistics(
                        rs.getInt("total_payments"),
                        rs.getInt("paid_count"),
                        rs.getInt("overdue_count"),
                        rs.getInt("ontime_count")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new PaymentStatistics(0, 0, 0, 0);
    }

    // Вспомогательный метод для маппинга
    private Payment mapResultSetToPayment(ResultSet rs) throws SQLException {
        Payment payment = new Payment();
        payment.setId(rs.getInt("id"));
        payment.setCreditId(rs.getInt("credit_id"));
        payment.setPlannedDate(rs.getDate("planned_date").toLocalDate());
        payment.setPlannedAmount(rs.getBigDecimal("planned_amount"));

        Date actualDate = rs.getDate("actual_date");
        if (actualDate != null) {
            payment.setActualDate(actualDate.toLocalDate());
        }

        payment.setActualAmount(rs.getBigDecimal("actual_amount"));
        payment.setStatus(PaymentStatus.valueOf(rs.getString("status")));

        return payment;
    }

    // Внутренний класс для статистики платежей
    public static class PaymentStatistics {
        private final int totalPayments;
        private final int paidCount;
        private final int overdueCount;
        private final int onTimeCount;

        public PaymentStatistics(int totalPayments, int paidCount, int overdueCount, int onTimeCount) {
            this.totalPayments = totalPayments;
            this.paidCount = paidCount;
            this.overdueCount = overdueCount;
            this.onTimeCount = onTimeCount;
        }

        public int getTotalPayments() { return totalPayments; }
        public int getPaidCount() { return paidCount; }
        public int getOverdueCount() { return overdueCount; }
        public int getOnTimeCount() { return onTimeCount; }

        public double getOnTimePercentage() {
            if (totalPayments == 0) return 0;
            return (onTimeCount * 100.0) / totalPayments;
        }
    }
}