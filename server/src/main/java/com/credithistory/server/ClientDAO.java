package com.credithistory.server;


import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ClientDAO {


    public static void ensureSchema() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement()) {
            st.executeUpdate("ALTER TABLE clients ADD COLUMN birth_year INT NULL");
        } catch (SQLException e) {
            boolean duplicate = e.getErrorCode() == 1060
                    || (e.getMessage() != null && e.getMessage().contains("Duplicate column"));
            if (!duplicate) {
                e.printStackTrace();
            }
        }
    }

    public List<Client> getAllClients() {
        List<Client> clients = new ArrayList<>();
        String sql = "SELECT * FROM clients ORDER BY full_name";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Client client = mapResultSetToClient(rs);
                clients.add(client);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return clients;
    }

    public Client findById(int id) {
        String sql = "SELECT * FROM clients WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToClient(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Client> searchByName(String name) {
        List<Client> clients = new ArrayList<>();
        String sql = "SELECT * FROM clients WHERE full_name LIKE ? ORDER BY full_name";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, "%" + name + "%");
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                clients.add(mapResultSetToClient(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return clients;
    }

    public boolean createClient(Client client) {
        String sql = "INSERT INTO clients (full_name, passport, phone, email, address, registered_by, birth_year) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, client.getFullName());
            stmt.setString(2, client.getPassport());
            stmt.setString(3, client.getPhone());
            stmt.setString(4, client.getEmail());
            stmt.setString(5, client.getAddress());
            stmt.setInt(6, client.getRegisteredBy());
            if (client.getBirthYear() != null) {
                stmt.setInt(7, client.getBirthYear());
            } else {
                stmt.setNull(7, Types.INTEGER);
            }

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    client.setId(generatedKeys.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean updateClient(Client client) {
        String sql = "UPDATE clients SET full_name=?, passport=?, phone=?, email=?, address=?, birth_year=? WHERE id=?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, client.getFullName());
            stmt.setString(2, client.getPassport());
            stmt.setString(3, client.getPhone());
            stmt.setString(4, client.getEmail());
            stmt.setString(5, client.getAddress());
            if (client.getBirthYear() != null) {
                stmt.setInt(6, client.getBirthYear());
            } else {
                stmt.setNull(6, Types.INTEGER);
            }
            stmt.setInt(7, client.getId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteClient(int id) {
        String sql = "DELETE FROM clients WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private Client mapResultSetToClient(ResultSet rs) throws SQLException {
        Client client = new Client();
        client.setId(rs.getInt("id"));
        client.setFullName(rs.getString("full_name"));
        client.setPassport(rs.getString("passport"));
        client.setPhone(rs.getString("phone"));
        client.setEmail(rs.getString("email"));
        client.setAddress(rs.getString("address"));
        client.setRegisteredBy(rs.getInt("registered_by"));
        client.setCreatedAt(rs.getTimestamp("created_at"));
        try {
            int by = rs.getInt("birth_year");
            client.setBirthYear(rs.wasNull() ? null : by);
        } catch (SQLException ex) {
            client.setBirthYear(null);
        }
        return client;
    }

    public int countClientsRegisteredByUser(int userId) {
        String sql = "SELECT COUNT(*) FROM clients WHERE registered_by = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
    public ClientStatistics getClientStatistics(int clientId) {
        String sql = "SELECT " +
                "c.id, c.full_name, c.created_at, " +
                "(SELECT COUNT(*) FROM credits WHERE client_id = c.id) as total_credits, " +
                "(SELECT COUNT(*) FROM credits WHERE client_id = c.id AND status = 'ACTIVE') as active_credits, " +
                "(SELECT COUNT(*) FROM credits WHERE client_id = c.id AND status = 'CLOSED') as closed_credits, " +
                "(SELECT COUNT(*) FROM payments p JOIN credits cr ON p.credit_id = cr.id WHERE cr.client_id = c.id AND p.status = 'PAID') as total_paid, " +
                "(SELECT COUNT(*) FROM payments p JOIN credits cr ON p.credit_id = cr.id WHERE cr.client_id = c.id AND p.status = 'PAID' AND p.actual_date <= p.planned_date) as paid_ontime, " +
                "(SELECT COUNT(*) FROM payments p JOIN credits cr ON p.credit_id = cr.id WHERE cr.client_id = c.id AND p.status = 'OVERDUE') as total_overdue, " +
                "(SELECT COUNT(*) FROM payments p JOIN credits cr ON p.credit_id = cr.id WHERE cr.client_id = c.id AND p.actual_amount > p.planned_amount) as early_payments " +
                "FROM clients c WHERE c.id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, clientId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                ClientStatistics stats = new ClientStatistics();
                stats.setClientId(rs.getInt("id"));
                stats.setFullName(rs.getString("full_name"));
                stats.setCreatedAt(rs.getTimestamp("created_at"));
                stats.setTotalCredits(rs.getInt("total_credits"));
                stats.setActiveCredits(rs.getInt("active_credits"));
                stats.setClosedCredits(rs.getInt("closed_credits"));
                stats.setTotalPaid(rs.getInt("total_paid"));
                stats.setPaidOnTime(rs.getInt("paid_ontime"));
                stats.setTotalOverdue(rs.getInt("total_overdue"));
                stats.setEarlyPayments(rs.getInt("early_payments"));
                return stats;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}