
package com.credithistory.service;

import com.credithistory.model.Role;
import com.credithistory.model.User;
import com.credithistory.util.LoggerUtil;
import org.apache.logging.log4j.Logger;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

import java.util.HashMap;
import java.util.Map;

public class UserService {

    private static final Logger logger = LoggerUtil.getLogger(UserService.class);

    private final Map<String, User> users = new ConcurrentHashMap<>();

    public UserService() {
        // создаем супер админа
        User superAdmin = new User(
                0,                          // id
                "root",                     // login
                "root123",                  // password
                "Super Admin",              // fullName
                Role.SUPER_ADMIN            // role
        );


        users.put(superAdmin.getLogin(), superAdmin);
        logger.info("Super admin created");
    }

    public String register(String login, String password) {
        if (users.containsKey(login)) {
            return "error: user already exists";
        }

        User user = new User(
                0,                          // id
                login,                      // login
                password,                   // password
                "User " + login,            // fullName
                Role.USER                   // role
        );
        users.put(login, user);

        logger.info("User registered: {}", login);
        return "success: user registered";
    }

    public String login(String login, String password) {
        User user = users.get(login);

        if (user == null) {
            return "error: user not found";
        }

        if (!user.getPassword().equals(password)) {
            return "error: wrong password";
        }

        logger.info("User logged in: {}", login);

        return "success: logged in as " + user.getRole();
    }

    public User getUser(String login) {
        return users.get(login);
    }
    public boolean hasRole(String login, Role requiredRole) {
        User user = users.get(login);
        if (user == null) return false;

        // SUPER_ADMIN имеет все права
        if (user.getRole() == Role.SUPER_ADMIN) return true;

        return user.getRole() == requiredRole;
    }
}