package com.madcamp.moody.playlist;

import com.madcamp.moody.music.MusicDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlaylistDTO {
    private Long playlistId;
    private String title;
    private Long userId; // diaryId 필드를 userId로 변경
    private LocalDate date;
    private List<MusicDTO> musics;

    // 생성자 (레거시 코드 호환용)
    public PlaylistDTO(String title, Long userId, LocalDate date) {
        this.title = title;
        this.userId = userId;
        this.date = date;
    }

    // 레거시 호환성을 위한 getDiaryId 메소드
    public Long getDiaryId() {
        return this.userId;
    }

    // 레거시 호환성을 위한 setDiaryId 메소드
    public void setDiaryId(Long diaryId) {
        this.userId = diaryId;
    }

    public static PlaylistDTO fromEntity(Playlist playlist) {
        if (playlist == null) {
            return null;
        }

        return new PlaylistDTO(
                playlist.getPlaylistId(),
                playlist.getTitle(),
                playlist.getDiaryId(), // diaryId 필드 직접 사용
                playlist.getDate(),
                new ArrayList<>() // musics는 별도로 로드
        );
    }
} 