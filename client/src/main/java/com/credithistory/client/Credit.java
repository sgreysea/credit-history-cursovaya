package com.credithistory.client;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;

public class Credit implements Serializable {

    private int id;
    private int clientId;
    private int userId;
    private BigDecimal amount;
    private int termMonths;
    private BigDecimal interestRate;
    private LocalDate issueDate;
    private CreditStatus status;
    private Timestamp createdAt;

    public Credit() {}

    public Credit(int clientId, int userId, BigDecimal amount,
                  int termMonths, BigDecimal interestRate, LocalDate issueDate) {
        this.clientId = clientId;
        this.userId = userId;
        this.amount = amount;
        this.termMonths = termMonths;
        this.interestRate = interestRate;
        this.issueDate = issueDate;
        this.status = CreditStatus.ACTIVE;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getClientId() { return clientId; }
    public void setClientId(int clientId) { this.clientId = clientId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public int getTermMonths() { return termMonths; }
    public void setTermMonths(int termMonths) { this.termMonths = termMonths; }

    public BigDecimal getInterestRate() { return interestRate; }
    public void setInterestRate(BigDecimal interestRate) { this.interestRate = interestRate; }

    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }

    public CreditStatus getStatus() { return status; }
    public void setStatus(CreditStatus status) { this.status = status; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public BigDecimal getMonthlyPayment() {
        double monthlyRate = interestRate.doubleValue() / 100 / 12;
        double amountDouble = amount.doubleValue();
        double payment = amountDouble * monthlyRate * Math.pow(1 + monthlyRate, termMonths)
                / (Math.pow(1 + monthlyRate, termMonths) - 1);
        return BigDecimal.valueOf(payment);
    }

    @Override
    public String toString() {
        return "Credit #" + id + " - " + amount + " BYN";
    }
    // дострочное погашение которое не работет)))))))))))
    public void recalculateRemainingPayments(BigDecimal extraPayment) {
        // тут наверне в paymentdao
    }
}