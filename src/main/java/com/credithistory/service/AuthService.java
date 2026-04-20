<<<<<<< HEAD
package com.credithistory.service;
public class AuthService {

    private static final String SUPER_ADMIN_LOGIN = "root";
    private static final String SUPER_ADMIN_PASSWORD = "root123";

    public boolean isSuperAdmin(String login, String password) {
        return SUPER_ADMIN_LOGIN.equals(login) &&
                SUPER_ADMIN_PASSWORD.equals(password);
    }
=======
package com.credithistory.service;
public class AuthService {

    private static final String SUPER_ADMIN_LOGIN = "root";
    private static final String SUPER_ADMIN_PASSWORD = "root123";

    public boolean isSuperAdmin(String login, String password) {
        return SUPER_ADMIN_LOGIN.equals(login) &&
                SUPER_ADMIN_PASSWORD.equals(password);
    }
>>>>>>> 9a25b7675c45b2149c90b056a1d7d77d419d7ecd
}