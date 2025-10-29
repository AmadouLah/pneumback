package com.pneumaliback.www.enums;

public enum PaymentMethod {
    ORANGE_MONEY("Orange Money"),
    MALITEL_MONEY("Malitel Money"), 
    MOOV_MONEY("Moov Money"),
    BANK_CARD("Carte bancaire"),
    PAYPAL("PayPal"),
    CASH_ON_DELIVERY("Paiement à la livraison");
    
    private final String displayName;
    
    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public boolean isMobileMoney() {
        return this == ORANGE_MONEY || this == MALITEL_MONEY || this == MOOV_MONEY;
    }
}