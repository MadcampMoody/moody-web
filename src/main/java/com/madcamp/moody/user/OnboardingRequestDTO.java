package com.madcamp.moody.user;

import com.madcamp.moody.music.MusicRegion;
import java.util.List;

public class OnboardingRequestDTO {
    private String name;
    private MusicRegion musicRegion;
    private List<String> musicGenres;

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public MusicRegion getMusicRegion() {
        return musicRegion;
    }

    public void setMusicRegion(MusicRegion musicRegion) {
        this.musicRegion = musicRegion;
    }

    public List<String> getMusicGenres() {
        return musicGenres;
    }

    public void setMusicGenres(List<String> musicGenres) {
        this.musicGenres = musicGenres;
    }
} 