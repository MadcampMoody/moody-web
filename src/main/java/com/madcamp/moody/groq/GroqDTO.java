package com.madcamp.moody.groq;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.List;

public class GroqDTO {
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GroqRequest {
        private String model;
        private List<Message> messages;
        @JsonProperty("max_tokens")
        private Integer maxTokens;
        private Double temperature;
        
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Message {
            private String role;
            private String content;
        }
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GroqResponse {
        private String id;
        private String object;
        private Long created;
        private String model;
        private List<Choice> choices;
        private Usage usage;
        
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Choice {
            private Integer index;
            private Message message;
            @JsonProperty("finish_reason")
            private String finishReason;
            
            @Data
            @NoArgsConstructor
            @AllArgsConstructor
            public static class Message {
                private String role;
                private String content;
            }
        }
        
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Usage {
            @JsonProperty("prompt_tokens")
            private Integer promptTokens;
            @JsonProperty("completion_tokens")
            private Integer completionTokens;
            @JsonProperty("total_tokens")
            private Integer totalTokens;
        }
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SimpleRequest {
        private String prompt;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SimpleResponse {
        private String response;
    }
    
    // AI 분석 결과를 담는 DTO
    public static class SpotifyAnalysisResult {
        @JsonProperty("genres")
        private List<String> genres;
        @JsonProperty("keywords")
        private List<String> keywords;
        
        // Jackson 생성자
        @JsonCreator
        public SpotifyAnalysisResult(@JsonProperty("genres") List<String> genres, 
                                   @JsonProperty("keywords") List<String> keywords) {
            this.genres = genres;
            this.keywords = keywords;
        }
        
        // Getters and Setters
        public List<String> getGenres() { return genres; }
        public void setGenres(List<String> genres) { this.genres = genres; }
        public List<String> getKeywords() { return keywords; }
        public void setKeywords(List<String> keywords) { this.keywords = keywords; }
    }
    
    // 텍스트 분석 및 추천 결과를 담는 DTO
    public static class MusicAnalysisResponse {
        private SpotifyAnalysisResult analysis;
        private List<RecommendedTrack> tracks;

        public MusicAnalysisResponse(SpotifyAnalysisResult analysis, List<RecommendedTrack> tracks) {
            this.analysis = analysis;
            this.tracks = tracks;
        }

        // Getters and Setters
        public SpotifyAnalysisResult getAnalysis() { return analysis; }
        public void setAnalysis(SpotifyAnalysisResult analysis) { this.analysis = analysis; }
        public List<RecommendedTrack> getTracks() { return tracks; }
        public void setTracks(List<RecommendedTrack> tracks) { this.tracks = tracks; }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class RecommendedTrack {
            private String title;
            private String artist;
            private String spotifyUrl;
            private String previewUrl;
            private String trackId;
        }
    }
} 