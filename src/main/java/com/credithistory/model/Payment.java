<<<<<<< HEAD
package com.credithistory.model;

import java.time.LocalDate;

public class Payment {

    private Long id;
    private double amount;
    private LocalDate paymentDate;

    private PaymentStatus status;

    private Credit credit;

    public Payment(Long id, double amount, LocalDate paymentDate, Credit credit) {
        this.id = id;
        this.amount = amount;
        this.paymentDate = paymentDate;
        this.credit = credit;
        this.status = PaymentStatus.PLANNED;
    }

    public PaymentStatus getStatus() {
        return status;
    }
=======
package com.credithistory.model;

import java.time.LocalDate;

public class Payment {

    private Long id;
    private double amount;
    private LocalDate paymentDate;

    private PaymentStatus status;

    private Credit credit;

    public Payment(Long id, double amount, LocalDate paymentDate, Credit credit) {
        this.id = id;
        this.amount = amount;
        this.paymentDate = paymentDate;
        this.credit = credit;
        this.status = PaymentStatus.PLANNED;
    }

    public PaymentStatus getStatus() {
        return status;
    }
>>>>>>> 9a25b7675c45b2149c90b056a1d7d77d419d7ecd
}