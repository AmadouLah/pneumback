package com.pneumaliback.www.enums;

public enum Gender {
    HOMME("Homme"),
    FEMME("Femme"),
    AUTRE("Autre");

    private final String displayName;

    Gender(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
