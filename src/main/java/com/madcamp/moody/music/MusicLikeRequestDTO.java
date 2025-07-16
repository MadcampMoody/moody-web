package com.madcamp.moody.music;

import lombok.Data;

@Data
public class MusicLikeRequestDTO {
    private String spotifyUri;
    private String title;
    private String artist;
    private String albumArtUrl;
} 