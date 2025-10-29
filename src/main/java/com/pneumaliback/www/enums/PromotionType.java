package com.pneumaliback.www.enums;

public enum PromotionType {
    PERCENTAGE("Pourcentage"),
    FIXED_AMOUNT("Montant fixe"),
    BUY_ONE_GET_ONE("Achetez 1, obtenez 1"),
    INFLUENCER_CODE("Code influenceur");
    
    private final String displayName;
    
    PromotionType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
