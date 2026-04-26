package com.credithistory.client;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

public class Payment implements Serializable {

    private int id;
    private int creditId;
    private LocalDate plannedDate;
    private BigDecimal plannedAmount;
    private LocalDate actualDate;
    private BigDecimal actualAmount;
    private PaymentStatus status;

    public Payment() {}

    public Payment(int creditId, LocalDate plannedDate, BigDecimal plannedAmount) {
        this.creditId = creditId;
        this.plannedDate = plannedDate;
        this.plannedAmount = plannedAmount;
        this.status = PaymentStatus.PENDING;
    }


    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCreditId() { return creditId; }
    public void setCreditId(int creditId) { this.creditId = creditId; }

    public LocalDate getPlannedDate() { return plannedDate; }
    public void setPlannedDate(LocalDate plannedDate) { this.plannedDate = plannedDate; }

    public BigDecimal getPlannedAmount() { return plannedAmount; }
    public void setPlannedAmount(BigDecimal plannedAmount) { this.plannedAmount = plannedAmount; }

    public LocalDate getActualDate() { return actualDate; }
    public void setActualDate(LocalDate actualDate) { this.actualDate = actualDate; }

    public BigDecimal getActualAmount() { return actualAmount; }
    public void setActualAmount(BigDecimal actualAmount) { this.actualAmount = actualAmount; }

    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }

    public boolean isOverdue() {
        return status == PaymentStatus.PENDING && LocalDate.now().isAfter(plannedDate);
    }

    public boolean isPaidOnTime() {
        return status == PaymentStatus.PAID && actualDate != null
                && !actualDate.isAfter(plannedDate);
    }

    @Override
    public String toString() {
        return plannedDate + " - " + plannedAmount + " BYN (" + status.getDisplayName() + ")";
    }
    public BigDecimal getPenalty() {
        if (status == PaymentStatus.OVERDUE && LocalDate.now().isAfter(plannedDate)) {
            long daysOverdue = java.time.temporal.ChronoUnit.DAYS.between(plannedDate, LocalDate.now());
            // Штраф 0.5% за каждый день просрочки
            return plannedAmount.multiply(BigDecimal.valueOf(0.005 * daysOverdue));
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal getTotalAmountWithPenalty() {
        return plannedAmount.add(getPenalty());
    }
}