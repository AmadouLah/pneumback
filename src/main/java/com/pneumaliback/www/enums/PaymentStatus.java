package com.pneumaliback.www.enums;

public enum PaymentStatus {
    PENDING("En attente"),
    PROCESSING("En cours"),
    SUCCESS("Succès"),
    FAILED("Échec"),
    REFUNDED("Remboursé"),
    EXPIRED("Expiré");
    
    private final String displayName;
    
    PaymentStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public boolean isCompleted() {
        return this == SUCCESS || this == FAILED || this == REFUNDED;
    }
}
