package com.credithistory.client;

public enum PaymentStatus {
    PENDING("Ожидается"),
    PAID("Оплачен"),
    OVERDUE("Просрочен");

    private final String displayName;

    PaymentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}