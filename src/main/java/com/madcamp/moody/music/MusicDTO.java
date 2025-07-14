package com.madcamp.moody.music;

public class MusicDTO {
    private Long musicId;
    private String musicUrl;
    private Long playlistId;
    private Long userId;

    // 기본 생성자
    public MusicDTO() {
    }

    // 생성자
    public MusicDTO(String musicUrl, Long playlistId) {
        this.musicUrl = musicUrl;
        this.playlistId = playlistId;
    }

    // 모든 필드 생성자
    public MusicDTO(Long musicId, String musicUrl, Long playlistId) {
        this.musicId = musicId;
        this.musicUrl = musicUrl;
        this.playlistId = playlistId;
    }

    // Entity에서 DTO로 변환하는 정적 메서드
    public static MusicDTO fromEntity(Music music) {
        return new MusicDTO(
            music.getMusicId(),
            music.getMusicUrl(),
            music.getPlaylistId()
        );
    }

    // DTO에서 Entity로 변환하는 메서드
    public Music toEntity() {
        Music music = new Music();
        music.setMusicId(this.musicId);
        music.setMusicUrl(this.musicUrl);
        music.setPlaylistId(this.playlistId);
        return music;
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    @Override
    public String toString() {
        return "MusicDTO{" +
                "musicId=" + musicId +
                ", musicUrl='" + musicUrl + '\'' +
                ", playlistId=" + playlistId +
                '}';
    }
} 