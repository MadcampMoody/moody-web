package com.madcamp.moody.playlist;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.LocalDate;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "playlist")
public class Playlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "playlist_id")
    private Long playlistId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "diary_id", nullable = false)
    private Long diaryId;

    @Column(name = "date")
    private LocalDate date;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 기본 생성자
    public Playlist() {
    }

    // 생성자
    public Playlist(String title, Long diaryId) {
        this.title = title;
        this.diaryId = diaryId;
    }

    // 생성자 (date 포함)
    public Playlist(String title, Long diaryId, LocalDate date) {
        this.title = title;
        this.diaryId = diaryId;
        this.date = date;
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

    @Override
    public String toString() {
        return "Playlist{" +
                "playlistId=" + playlistId +
                ", title='" + title + '\'' +
                ", diaryId=" + diaryId +
                ", date=" + date +
                ", createdAt=" + createdAt +
                '}';
    }
} 