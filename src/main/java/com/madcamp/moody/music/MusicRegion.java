package com.madcamp.moody.music;

public enum MusicRegion {
    DOMESTIC("domestic", "국내음악"),
    INTERNATIONAL("international", "해외음악"),
    BOTH("both", "둘 다");
    
    private final String value;
    private final String displayName;
    
    MusicRegion(String value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }
    
    public String getValue() {
        return value;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public static MusicRegion fromValue(String value) {
        for (MusicRegion region : values()) {
            if (region.value.equals(value)) {
                return region;
            }
        }
        return null;
    }
} 