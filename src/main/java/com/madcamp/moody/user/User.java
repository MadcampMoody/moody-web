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