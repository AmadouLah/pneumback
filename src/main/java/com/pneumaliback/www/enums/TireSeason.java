package com.pneumaliback.www.enums;

public enum TireSeason {
   ETE("Été"),
    HIVER("Hiver"),
    QUATRE_SAISONS("4 saisons"),
    TOUT_TERRAIN("Tout-terrain");
    
    private final String displayName;
    
    TireSeason(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
