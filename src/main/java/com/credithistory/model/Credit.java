
package com.credithistory.model;

public class Credit {

    private Long id;
    private double amount;
    private double interestRate;
    private int termMonths;

    private CreditStatus status;

    private User borrower;

    public Credit(Long id, double amount, double interestRate, int termMonths, User borrower) {
        this.id = id;
        this.amount = amount;
        this.interestRate = interestRate;
        this.termMonths = termMonths;
        this.borrower = borrower;
        this.status = CreditStatus.ACTIVE;
    }

    public CreditStatus getStatus() {
        return status;
    }

    public User getBorrower() {
        return borrower;
    }
}