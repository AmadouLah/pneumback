package com.pneumaliback.www.enums;

public enum Country {
    MALI("Mali", "ML", "+223"),
    MOROCCO("Maroc", "MA", "+212"),
    BURKINA_FASO("Burkina Faso", "BF", "+226"),
    SENEGAL("Sénégal", "SN", "+221"),
    IVORY_COAST("Côte d'Ivoire", "CI", "+225");
    
    private final String displayName;
    private final String countryCode;
    private final String phonePrefix;
    
    Country(String displayName, String countryCode, String phonePrefix) {
        this.displayName = displayName;
        this.countryCode = countryCode;
        this.phonePrefix = phonePrefix;
    }
    
    public String getDisplayName() { return displayName; }
    public String getCountryCode() { return countryCode; }
    public String getPhonePrefix() { return phonePrefix; }
}
