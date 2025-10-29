package com.pneumaliback.www.enums;

public enum VehicleType {
    CITADINE("Citadine"),
    BERLINE("Berline"),
    SUV("SUV/4x4"),
    PICKUP("Pick-up"),
    CAMION("Camion"),
    MOTO("Moto");
    
    private final String displayName;
    
    VehicleType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
