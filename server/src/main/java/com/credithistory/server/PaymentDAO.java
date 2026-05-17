package com.credithistory.server;


import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.math.RoundingMode;

public class PaymentDAO {


    // получить платеж по id
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

    // график платеж для кредита
    public boolean generatePaymentSchedule(int creditId, BigDecimal monthlyPayment,
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

    public boolean skipPayment(int paymentId) {
        Payment payment = findById(paymentId);
        if (payment == null || payment.getStatus() != PaymentStatus.PENDING) {
            return false;
        }
        BigDecimal overdueDebt = payment.getPlannedAmount()
                .multiply(BigDecimal.valueOf(1.005))
                .setScale(2, RoundingMode.HALF_UP);
        String markOverdueSql = "UPDATE payments SET status = 'OVERDUE', actual_date = ?, actual_amount = ?, planned_amount = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(markOverdueSql)) {
            conn.setAutoCommit(false);
            stmt.setDate(1, Date.valueOf(LocalDate.now()));
            stmt.setBigDecimal(2, BigDecimal.ZERO);
            stmt.setBigDecimal(3, overdueDebt);
            stmt.setInt(4, paymentId);
            int updated = stmt.executeUpdate();
            if (updated == 0) {
                conn.rollback();
                return false;
            }
            recalculateFuturePayments(conn, payment.getCreditId(), overdueDebt);
            conn.commit();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean markAsPaid(int paymentId, BigDecimal actualAmount) {
        Payment payment = findById(paymentId);
        if (payment == null || payment.getStatus() == PaymentStatus.PAID) {
            return false;
        }
        String sql = "UPDATE payments SET status = 'PAID', actual_date = ?, actual_amount = ? WHERE id = ?";
        BigDecimal extraAmount = actualAmount.subtract(payment.getPlannedAmount());
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            stmt.setDate(1, Date.valueOf(LocalDate.now()));
            stmt.setBigDecimal(2, actualAmount);
            stmt.setInt(3, paymentId);
            if (stmt.executeUpdate() == 0) {
                conn.rollback();
                return false;
            }
            if (extraAmount.compareTo(BigDecimal.ZERO) != 0) {
                // Перерасчет остатка: переплата уменьшает будущие суммы, недоплата увеличивает.
                recalculateFuturePayments(conn, payment.getCreditId(), extraAmount.negate());
            }
            settleIfFullyPaidOff(conn, payment.getCreditId());
            conn.commit();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // het просроченные платежи
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

    // пометить просроченные платежи
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

    // получка статистики по платежам клиента
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

    // внутр класс для статических платежей платежей
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
    public void updateOverduePayments() {
        String sql = "UPDATE payments SET status = 'OVERDUE' " +
                "WHERE status = 'PENDING' AND planned_date < CURDATE()";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            int updated = stmt.executeUpdate();
            if (updated > 0) {
                System.out.println("Обновлено просроченных платежей: " + updated);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public LocalDate getLatestPaidActualDate(int creditId) {
        String sql = "SELECT MAX(actual_date) AS d FROM payments WHERE credit_id = ? AND status = 'PAID'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, creditId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Date d = rs.getDate("d");
                if (d != null) return d.toLocalDate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

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

    //ЧТОТО УТТ НЕ РАБОТАЕТ ЕМАЕ ЧЕ ДЕЛАТЬ
    public boolean makeEarlyPayment(int creditId, BigDecimal extraAmount) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            recalculateFuturePayments(conn, creditId, extraAmount.negate());
            conn.commit();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    public void recalculateFuturePayments(int creditId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            recalculateFuturePayments(conn, creditId, BigDecimal.ZERO);
            conn.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /** Если остаток по графику нулевой (или копейки), закрываем активные платежи и кредит. */
    private void settleIfFullyPaidOff(Connection conn, int creditId) throws SQLException {
        String zeroPending = "UPDATE payments SET status='PAID', actual_date=CURDATE(), actual_amount=planned_amount "
                + "WHERE credit_id=? AND status='PENDING' AND planned_amount<=0";
        try (PreparedStatement ps = conn.prepareStatement(zeroPending)) {
            ps.setInt(1, creditId);
            ps.executeUpdate();
        }

        String sumPending = "SELECT COALESCE(SUM(planned_amount), 0) FROM payments WHERE credit_id=? AND status='PENDING'";
        BigDecimal remaining = BigDecimal.ZERO;
        try (PreparedStatement ps = conn.prepareStatement(sumPending)) {
            ps.setInt(1, creditId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) remaining = rs.getBigDecimal(1);
        }

        BigDecimal waiveLimit = new BigDecimal("0.05");
        if (remaining.compareTo(BigDecimal.ZERO) > 0 && remaining.compareTo(waiveLimit) <= 0) {
            String waive = "UPDATE payments SET status='PAID', actual_date=CURDATE(), actual_amount=planned_amount "
                    + "WHERE credit_id=? AND status='PENDING'";
            try (PreparedStatement ps = conn.prepareStatement(waive)) {
                ps.setInt(1, creditId);
                ps.executeUpdate();
            }
        }

        String countPen = "SELECT COUNT(*) FROM payments WHERE credit_id=? AND status='PENDING'";
        int pendingCnt = 0;
        try (PreparedStatement ps = conn.prepareStatement(countPen)) {
            ps.setInt(1, creditId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) pendingCnt = rs.getInt(1);
        }
        if (pendingCnt == 0) {
            String close = "UPDATE credits SET status='CLOSED' WHERE id=? AND status != 'CLOSED'";
            try (PreparedStatement ps = conn.prepareStatement(close)) {
                ps.setInt(1, creditId);
                ps.executeUpdate();
            }
        }
    }

    public void addExtraPayment(int creditId, LocalDate date, BigDecimal amount) {
        String sql = "INSERT INTO payments (credit_id, planned_date, planned_amount, status) VALUES (?, ?, ?, 'PENDING')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, creditId);
            stmt.setDate(2, Date.valueOf(date));
            stmt.setBigDecimal(3, amount);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    private void recalculateFuturePayments(Connection conn, int creditId, BigDecimal debtDelta) throws SQLException {
        List<Payment> pendingPayments = new ArrayList<>();
        String selectSQL = "SELECT * FROM payments WHERE credit_id = ? AND status = 'PENDING' ORDER BY planned_date";
        try (PreparedStatement stmt = conn.prepareStatement(selectSQL)) {
            stmt.setInt(1, creditId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                pendingPayments.add(mapResultSetToPayment(rs));
            }
        }
        if (pendingPayments.isEmpty()) return;

        BigDecimal totalPending = BigDecimal.ZERO;
        for (Payment p : pendingPayments) {
            totalPending = totalPending.add(p.getPlannedAmount());
        }
        BigDecimal newTotal = totalPending.add(debtDelta);
        if (newTotal.compareTo(BigDecimal.ZERO) < 0) {
            newTotal = BigDecimal.ZERO;
        }

        BigDecimal count = BigDecimal.valueOf(pendingPayments.size());
        BigDecimal base = newTotal.divide(count, 2, RoundingMode.DOWN);
        BigDecimal distributed = base.multiply(count);
        BigDecimal remainder = newTotal.subtract(distributed);

        String updateSQL = "UPDATE payments SET planned_amount = ? WHERE id = ?";
        try (PreparedStatement updateStmt = conn.prepareStatement(updateSQL)) {
            for (int i = 0; i < pendingPayments.size(); i++) {
                Payment p = pendingPayments.get(i);
                BigDecimal amount = base;
                if (i == pendingPayments.size() - 1) {
                    amount = amount.add(remainder).setScale(2, RoundingMode.HALF_UP);
                }
                updateStmt.setBigDecimal(1, amount);
                updateStmt.setInt(2, p.getId());
                updateStmt.addBatch();
            }
            updateStmt.executeBatch();
        }
    }
}