package com.madcamp.moody.liked;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LikedSongRequestDTO {
    private String trackId;
    private String musicUrl;
    private String title;
    private String artist;
} 