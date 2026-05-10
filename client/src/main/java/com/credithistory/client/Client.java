package com.credithistory.client;

import java.io.Serializable;
import java.sql.Timestamp;

public class Client implements Serializable {

    private int id;
    private String fullName;
    private String passport;
    private String phone;
    private String email;
    private String address;
    private Integer birthYear;
    private int registeredBy;
    private Timestamp createdAt;
    /** подгружается из get_clients, для отображения */
    private int creditCount;

    public int getCreditCount() { return creditCount; }
    public void setCreditCount(int creditCount) { this.creditCount = creditCount; }

    public Integer getBirthYear() { return birthYear; }
    public void setBirthYear(Integer birthYear) { this.birthYear = birthYear; }
    private String ratingLetter;
    public String getRatingLetter() { return ratingLetter; }
    public void setRatingLetter(String ratingLetter) { this.ratingLetter = ratingLetter; }

    public Client() {}

    public Client(String fullName, String passport, String phone, int registeredBy) {
        this.fullName = fullName;
        this.passport = passport;
        this.phone = phone;
        this.registeredBy = registeredBy;
    }

    // Геттеры и сеттеры
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPassport() { return passport; }
    public void setPassport(String passport) { this.passport = passport; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public int getRegisteredBy() { return registeredBy; }
    public void setRegisteredBy(int registeredBy) { this.registeredBy = registeredBy; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return fullName + " (" + passport + ")";
    }
}