package com.madcamp.moody.playlist;

import com.madcamp.moody.music.MusicDTO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class PlaylistDTO {
    private Long playlistId;
    private String title;
    private Long diaryId;
    private LocalDate date;
    private LocalDateTime createdAt;
    private List<MusicDTO> musics; // 음악 목록 추가

    // 기본 생성자
    public PlaylistDTO() {
    }

    // 생성자
    public PlaylistDTO(String title, Long diaryId) {
        this.title = title;
        this.diaryId = diaryId;
    }

    // 생성자 (date 포함)
    public PlaylistDTO(String title, Long diaryId, LocalDate date) {
        this.title = title;
        this.diaryId = diaryId;
        this.date = date;
    }

    // 모든 필드 생성자
    public PlaylistDTO(Long playlistId, String title, Long diaryId, LocalDate date, LocalDateTime createdAt) {
        this.playlistId = playlistId;
        this.title = title;
        this.diaryId = diaryId;
        this.date = date;
        this.createdAt = createdAt;
    }

    // Entity에서 DTO로 변환하는 정적 메서드
    public static PlaylistDTO fromEntity(Playlist playlist) {
        return new PlaylistDTO(
            playlist.getPlaylistId(),
            playlist.getTitle(),
            playlist.getDiaryId(),
            playlist.getDate(),
            playlist.getCreatedAt()
        );
    }

    // DTO에서 Entity로 변환하는 메서드
    public Playlist toEntity() {
        Playlist playlist = new Playlist();
        playlist.setPlaylistId(this.playlistId);
        playlist.setTitle(this.title);
        playlist.setDiaryId(this.diaryId);
        playlist.setDate(this.date);
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

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<MusicDTO> getMusics() {
        return musics;
    }

    public void setMusics(List<MusicDTO> musics) {
        this.musics = musics;
    }

    @Override
    public String toString() {
        return "PlaylistDTO{" +
                "playlistId=" + playlistId +
                ", title='" + title + '\'' +
                ", diaryId=" + diaryId +
                ", date=" + date +
                ", createdAt=" + createdAt +
                '}';
    }
} 