package com.madcamp.moody.user;

import lombok.*;

import jakarta.persistence.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.madcamp.moody.music.MusicRegion;

import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "user")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    private String name;

    private String email;

    @Column(name = "oauth_id", nullable = false)
    private String oauthId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "music_region")
    private MusicRegion musicRegion;
    
    @Column(name = "music_genres", columnDefinition = "TEXT")
    private String musicGenres; // JSON 형태로 여러 장르 저장
    
    @Column(name = "onboarding_completed", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean onboardingCompleted = false;
    
    // Spotify 관련 필드들
    @Column(name = "spotify_oauth_id")
    private String spotifyOauthId;
    
    @Column(name = "spotify_email")
    private String spotifyEmail;
    
    @Column(name = "spotify_display_name")
    private String spotifyDisplayName;
    
    @Column(name = "spotify_access_token", columnDefinition = "TEXT")
    private String spotifyAccessToken;
    
    @Column(name = "spotify_refresh_token", columnDefinition = "TEXT")
    private String spotifyRefreshToken;
    
    // Spotify 연동 여부 확인 헬퍼 메서드
    public boolean isSpotifyLinked() {
        return spotifyOauthId != null && !spotifyOauthId.isEmpty();
    }
    
    // 음악 장르 목록을 가져오는 헬퍼 메서드
    public List<String> getMusicGenresList() {
        if (musicGenres == null || musicGenres.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(musicGenres, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
    
    // 음악 장르 목록을 설정하는 헬퍼 메서드
    public void setMusicGenresList(List<String> genres) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            this.musicGenres = objectMapper.writeValueAsString(genres);
        } catch (Exception e) {
            this.musicGenres = "[]";
        }
    }
    
}