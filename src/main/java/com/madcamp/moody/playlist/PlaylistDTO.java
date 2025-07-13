package com.madcamp.moody.playlist;

import java.time.LocalDateTime;

public class PlaylistDTO {
    private Long playlistId;
    private String title;
    private Long diaryId;
    private LocalDateTime createdAt;

    // 기본 생성자
    public PlaylistDTO() {
    }

    // 생성자
    public PlaylistDTO(String title, Long diaryId) {
        this.title = title;
        this.diaryId = diaryId;
    }

    // 모든 필드 생성자
    public PlaylistDTO(Long playlistId, String title, Long diaryId, LocalDateTime createdAt) {
        this.playlistId = playlistId;
        this.title = title;
        this.diaryId = diaryId;
        this.createdAt = createdAt;
    }

    // Entity에서 DTO로 변환하는 정적 메서드
    public static PlaylistDTO fromEntity(Playlist playlist) {
        return new PlaylistDTO(
            playlist.getPlaylistId(),
            playlist.getTitle(),
            playlist.getDiaryId(),
            playlist.getCreatedAt()
        );
    }

    // DTO에서 Entity로 변환하는 메서드
    public Playlist toEntity() {
        Playlist playlist = new Playlist();
        playlist.setPlaylistId(this.playlistId);
        playlist.setTitle(this.title);
        playlist.setDiaryId(this.diaryId);
        if (this.createdAt != null) {
            playlist.setCreatedAt(this.createdAt);
        }
        return playlist;
    }

    // Getter and Setter
    public Long getPlaylistId() {
        return playlistId;
    }

    public void setPlaylistId(Long playlistId) {
        this.playlistId = playlistId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getDiaryId() {
        return diaryId;
    }

    public void setDiaryId(Long diaryId) {
        this.diaryId = diaryId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "PlaylistDTO{" +
                "playlistId=" + playlistId +
                ", title='" + title + '\'' +
                ", diaryId=" + diaryId +
                ", createdAt=" + createdAt +
                '}';
    }
} 