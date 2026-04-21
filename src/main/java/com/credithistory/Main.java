package com.credithistory;

import com.credithistory.database.DatabaseConnection;
import com.credithistory.database.UserDAO;
import com.credithistory.model.User;

import java.sql.Connection;
import java.sql.SQLException;

public class Main {
    public static void main(String[] args) {
        try {
            Connection conn = DatabaseConnection.getConnection();
            System.out.println("✅ Подключение к MySQL успешно!");

            // Проверка: найти пользователя admin
            UserDAO userDAO = new UserDAO();
            User user = userDAO.findByLogin("admin");

            if (user != null) {
                System.out.println("✅ Найден пользователь: " + user.getFullName());
                System.out.println("   Роль: " + user.getRole());
            } else {
                System.out.println("❌ Пользователь admin не найден");
            }

        } catch (SQLException e) {
            System.err.println("❌ Ошибка подключения: " + e.getMessage());
            e.printStackTrace();
        }
    }
}