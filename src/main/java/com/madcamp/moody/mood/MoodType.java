package com.madcamp.moody.mood;

public enum MoodType {
    ANNOYED("짜증"),
    ANGRY("화남"),
    TIRED("피곤"),
    SAD("슬픔"),
    WORRIED("걱정"),
    BORED("지루"),
    HAPPY("행복"),
    CALM("편안"),
    EXCITED("설렘"),
    PROUD("뿌듯"),
    THANKFUL("감사");

    private final String displayName;

    MoodType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static MoodType fromDisplayName(String displayName) {
        for (MoodType mood : values()) {
            if (mood.displayName.equals(displayName)) {
                return mood;
            }
        }
        return null;
    }
}
