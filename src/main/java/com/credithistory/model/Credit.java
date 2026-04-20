<<<<<<< HEAD
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
=======
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
>>>>>>> 9a25b7675c45b2149c90b056a1d7d77d419d7ecd
}