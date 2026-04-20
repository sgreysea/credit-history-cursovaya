package com.credithistory.service;
public class AuthService {

    private static final String SUPER_ADMIN_LOGIN = "root";
    private static final String SUPER_ADMIN_PASSWORD = "root123";

    public boolean isSuperAdmin(String login, String password) {
        return SUPER_ADMIN_LOGIN.equals(login) &&
                SUPER_ADMIN_PASSWORD.equals(password);
    }
}