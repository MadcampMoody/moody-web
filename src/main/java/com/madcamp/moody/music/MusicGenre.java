package com.madcamp.moody.music;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;

public enum MusicGenre {
    pop("팝"),
    rock("락"),
    hip_hop("힙합"),
    r_n_b("R&B"),
    k_pop("k-pop"),
    jazz("재즈"),
    electronic("EDM"),
    country("컨트리"),
    dance("댄스"),
    indie("인디");
    
    private final String displayName;
    
    MusicGenre(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public static MusicGenre fromDisplayName(String displayName) {
        for (MusicGenre genre : values()) {
            if (genre.displayName.equals(displayName)) {
                return genre;
            }
        }
        return null;
    }

    public static MusicGenre convertToMusicGenre(String frontendGenre) {
        switch (frontendGenre) {
            case "팝": return MusicGenre.pop;
            case "락": return MusicGenre.rock;
            case "힙합": return MusicGenre.hip_hop;
            case "R&B": return MusicGenre.r_n_b;
            case "K-POP": return MusicGenre.k_pop;
            case "재즈": return MusicGenre.jazz;
            case "EDM": return MusicGenre.electronic;
            case "컨트리": return MusicGenre.country;
            case "댄스": return MusicGenre.dance;
            case "인디": return MusicGenre.indie;
            default:
                System.out.println("변환되지 않은 장르: " + frontendGenre);
                return null;
        }
    }
} 