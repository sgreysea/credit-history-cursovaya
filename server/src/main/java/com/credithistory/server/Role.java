package com.credithistory.server;

public enum Role {
    USER("Сотрудник"),
    ADMIN("Администратор"),
    SUPER_ADMIN("Супер-администратор");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}