package com.credithistory.model;

import java.time.LocalDate;

public class User {

    private Long id;
    private String fullName;
    private String passportNumber;
    private String email;
    private String phone;

    private String login;
    private String password;

    private Role role;

    private int creditScore;

    private LocalDate registrationDate;

    public User(Long id, String fullName, String passportNumber, String email,
                String phone, String login, String password, Role role) {
        this.id = id;
        this.fullName = fullName;
        this.passportNumber = passportNumber;
        this.email = email;
        this.phone = phone;
        this.login = login;
        this.password = password;
        this.role = role;
        this.registrationDate = LocalDate.now();
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public Role getRole() {
        return role;
    }

    public int getCreditScore() {
        return creditScore;
    }

    public void setCreditScore(int creditScore) {
        this.creditScore = creditScore;
    }
}