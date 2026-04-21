package com.credithistory.model;

public enum CreditStatus {
    ACTIVE("Активен"),
    CLOSED("Закрыт"),
    OVERDUE("Просрочен");

    private final String displayName;

    CreditStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}