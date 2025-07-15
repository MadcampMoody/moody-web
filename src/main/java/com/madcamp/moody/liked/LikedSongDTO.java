package com.madcamp.moody.liked;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class LikedSongDTO {
    private Long id;
    private String trackId;
    private String musicUrl;
    private String title;
    private String artist;
    private LocalDateTime createdAt;

    public static LikedSongDTO fromEntity(LikedSong entity) {
        LikedSongDTO dto = new LikedSongDTO();
        dto.setId(entity.getId());
        dto.setTrackId(entity.getTrackId());
        dto.setMusicUrl(entity.getMusicUrl());
        dto.setTitle(entity.getTitle());
        dto.setArtist(entity.getArtist());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }
} 