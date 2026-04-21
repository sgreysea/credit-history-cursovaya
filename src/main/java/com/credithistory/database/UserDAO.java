package com.credithistory.database;

import com.credithistory.model.Role;
import com.credithistory.model.User;
import java.sql.*;

public class UserDAO {

    public User findByLogin(String login) {
        String sql = "SELECT * FROM users WHERE login = ? AND is_active = true";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, login);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setLogin(rs.getString("login"));
                user.setPassword(rs.getString("password"));
                user.setFullName(rs.getString("full_name"));
                user.setRole(Role.valueOf(rs.getString("role")));
                return user;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean createUser(String login, String password, String fullName) {
        String sql = "INSERT INTO users (login, password, full_name, role) VALUES (?, ?, ?, 'USER')";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, login);
            stmt.setString(2, password);
            stmt.setString(3, fullName);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean checkPassword(String login, String password) {
        User user = findByLogin(login);
        return user != null && user.getPassword().equals(password);
    }
}