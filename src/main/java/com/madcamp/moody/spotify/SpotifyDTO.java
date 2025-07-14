package com.madcamp.moody.spotify;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class SpotifyDTO {
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenResponse {
        @JsonProperty("access_token")
        private String accessToken;
        @JsonProperty("token_type")
        private String tokenType;
        @JsonProperty("expires_in")
        private Integer expiresIn;
    }
    
    // 플레이리스트 검색 응답
    public static class PlaylistSearchResponse {
        private PagingObject<SimplifiedPlaylist> playlists;
        public PagingObject<SimplifiedPlaylist> getPlaylists() { return playlists; }
        public void setPlaylists(PagingObject<SimplifiedPlaylist> playlists) { this.playlists = playlists; }
    }

    // 플레이리스트 트랙 응답
    public static class PlaylistTracksResponse {
        private List<PlaylistTrack> items;
        // Getter and Setter
        public List<PlaylistTrack> getItems() { return items; }
        public void setItems(List<PlaylistTrack> items) { this.items = items; }
    }

    public static class PlaylistTrack {
        private Track track;
        // Getter and Setter
        public Track getTrack() { return track; }
        public void setTrack(Track track) { this.track = track; }
    }

    public static class PagingObject<T> {
        private List<T> items;
        public List<T> getItems() { return items; }
        public void setItems(List<T> items) { this.items = items; }
    }

    public static class SimplifiedPlaylist {
        private String id;
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
    }

    public static class Track {
        private String id;
        private String name;
        private List<Artist> artists;
        @JsonProperty("external_urls")
        private ExternalUrls externalUrls;
        @JsonProperty("preview_url")
        private String previewUrl;
        private int popularity;

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public List<Artist> getArtists() { return artists; }
        public void setArtists(List<Artist> artists) { this.artists = artists; }
        public ExternalUrls getExternalUrls() { return externalUrls; }
        public void setExternalUrls(ExternalUrls externalUrls) { this.externalUrls = externalUrls; }
        public String getPreviewUrl() { return previewUrl; }
        public void setPreviewUrl(String previewUrl) { this.previewUrl = previewUrl; }
        public int getPopularity() { return popularity; }
        public void setPopularity(int popularity) { this.popularity = popularity; }
    }

    public static class Artist {
        private String name;
        // Getter and Setter
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    public static class ExternalUrls {
        private String spotify;
        // Getter and Setter
        public String getSpotify() { return spotify; }
        public void setSpotify(String spotify) { this.spotify = spotify; }
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecommendationResponse {
        private List<Track> tracks;
    }
    
    // 최종 추천 결과를 담는 DTO
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MusicRecommendation {
        private List<RecommendedTrack> tracks;

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        public static class RecommendedTrack {
            private String title;
            private String artist;
            private String spotifyUrl;
            private String previewUrl;
            private String trackId;
            private int popularity;
        }
    }
} 