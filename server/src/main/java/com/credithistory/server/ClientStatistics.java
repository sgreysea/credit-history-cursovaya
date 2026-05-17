package com.credithistory.server;

public class ClientStatistics {
    private int clientId;
    private String fullName;
    private java.sql.Timestamp createdAt;
    private int totalCredits;
    private int activeCredits;
    private int closedCredits;
    private int totalPaid;
    private int paidOnTime;
    private int totalOverdue;
    private int earlyPayments;
    private int ratingScore;
    private String ratingLetter;
    private String ratingColor;


    public int getClientId() { return clientId; }
    public void setClientId(int clientId) { this.clientId = clientId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public java.sql.Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.sql.Timestamp createdAt) { this.createdAt = createdAt; }

    public int getTotalCredits() { return totalCredits; }
    public void setTotalCredits(int totalCredits) { this.totalCredits = totalCredits; }

    public int getActiveCredits() { return activeCredits; }
    public void setActiveCredits(int activeCredits) { this.activeCredits = activeCredits; }

    public int getClosedCredits() { return closedCredits; }
    public void setClosedCredits(int closedCredits) { this.closedCredits = closedCredits; }

    public int getTotalPaid() { return totalPaid; }
    public void setTotalPaid(int totalPaid) { this.totalPaid = totalPaid; }

    public int getPaidOnTime() { return paidOnTime; }
    public void setPaidOnTime(int paidOnTime) { this.paidOnTime = paidOnTime; }

    public int getTotalOverdue() { return totalOverdue; }
    public void setTotalOverdue(int totalOverdue) { this.totalOverdue = totalOverdue; }

    public int getEarlyPayments() { return earlyPayments; }
    public void setEarlyPayments(int earlyPayments) { this.earlyPayments = earlyPayments; }


    public int getLatePayments() {
        return totalPaid - paidOnTime;
    }

    public double getOnTimePercentage() {
        if (totalPaid == 0) return 0;
        return (paidOnTime * 100.0) / totalPaid;
    }

    public double getEarlyPercentage() {
        if (totalPaid == 0) return 0;
        return (earlyPayments * 100.0) / totalPaid;
    }

    public double getOverduePercentage() {
        int total = totalPaid + totalOverdue;
        if (total == 0) return 0;
        return (totalOverdue * 100.0) / total;
    }

    public int getRatingScore() { return ratingScore; }
    public void setRatingScore(int ratingScore) { this.ratingScore = ratingScore; }

    public String getRatingLetter() { return ratingLetter; }
    public void setRatingLetter(String ratingLetter) { this.ratingLetter = ratingLetter; }

    public String getRatingColor() { return ratingColor; }
    public void setRatingColor(String ratingColor) { this.ratingColor = ratingColor; }
}