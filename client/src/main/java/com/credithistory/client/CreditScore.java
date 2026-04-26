package com.credithistory.client;

import java.time.LocalDateTime;

public class CreditScore {

    private int id;
    private int clientId;
    private int score;
    private String category;
    private String description;
    private LocalDateTime calculatedAt;

    public CreditScore() {}

    public CreditScore(int clientId, int score, String category, String description) {
        this.clientId = clientId;
        this.score = score;
        this.category = category;
        this.description = description;
        this.calculatedAt = LocalDateTime.now();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getClientId() { return clientId; }
    public void setClientId(int clientId) { this.clientId = clientId; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCalculatedAt() { return calculatedAt; }
    public void setCalculatedAt(LocalDateTime calculatedAt) { this.calculatedAt = calculatedAt; }

    @Override
    public String toString() {
        return score + " (" + category + ")";
    }
}