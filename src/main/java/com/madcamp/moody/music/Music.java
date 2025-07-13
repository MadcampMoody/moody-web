package com.madcamp.moody.music;

import com.madcamp.moody.playlist.Playlist;
import jakarta.persistence.*;

@Entity
@Table(name = "music")
public class Music {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "music_id")
    private Long musicId;

    @Column(name = "music_url", nullable = false)
    private String musicUrl;

    @Column(name = "playlist_id", nullable = false)
    private Long playlistId;

    // Playlist와의 관계 (Many-to-One)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "playlist_id", insertable = false, updatable = false)
    private Playlist playlist;

    // 기본 생성자
    public Music() {
    }

    // 생성자
    public Music(String musicUrl, Long playlistId) {
        this.musicUrl = musicUrl;
        this.playlistId = playlistId;
    }

    // Getter and Setter
    public Long getMusicId() {
        return musicId;
    }

    public void setMusicId(Long musicId) {
        this.musicId = musicId;
    }

    public String getMusicUrl() {
        return musicUrl;
    }

    public void setMusicUrl(String musicUrl) {
        this.musicUrl = musicUrl;
    }

    public Long getPlaylistId() {
        return playlistId;
    }

    public void setPlaylistId(Long playlistId) {
        this.playlistId = playlistId;
    }

    public Playlist getPlaylist() {
        return playlist;
    }

    public void setPlaylist(Playlist playlist) {
        this.playlist = playlist;
    }

    @Override
    public String toString() {
        return "Music{" +
                "musicId=" + musicId +
                ", musicUrl='" + musicUrl + '\'' +
                ", playlistId=" + playlistId +
                '}';
    }
} 