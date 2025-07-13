package com.madcamp.moody.playlist;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class PlaylistService {

    private final PlaylistRepository playlistRepository;

    @Autowired
    public PlaylistService(PlaylistRepository playlistRepository) {
        this.playlistRepository = playlistRepository;
    }

    // 모든 playlist 조회
    @Transactional(readOnly = true)
    public List<PlaylistDTO> getAllPlaylists() {
        return playlistRepository.findAll()
                .stream()
                .map(PlaylistDTO::fromEntity)
                .collect(Collectors.toList());
    }

    // ID로 playlist 조회
    @Transactional(readOnly = true)
    public Optional<PlaylistDTO> getPlaylistById(Long playlistId) {
        return playlistRepository.findById(playlistId)
                .map(PlaylistDTO::fromEntity);
    }

    // diary_id로 playlist 조회
    @Transactional(readOnly = true)
    public List<PlaylistDTO> getPlaylistsByDiaryId(Long diaryId) {
        return playlistRepository.findByDiaryIdOrderByCreatedAtDesc(diaryId)
                .stream()
                .map(PlaylistDTO::fromEntity)
                .collect(Collectors.toList());
    }

    // title로 playlist 검색
    @Transactional(readOnly = true)
    public List<PlaylistDTO> searchPlaylistsByTitle(String title) {
        return playlistRepository.findByTitleContainingIgnoreCase(title)
                .stream()
                .map(PlaylistDTO::fromEntity)
                .collect(Collectors.toList());
    }

    // diary_id와 title로 playlist 검색
    @Transactional(readOnly = true)
    public List<PlaylistDTO> searchPlaylistsByDiaryIdAndTitle(Long diaryId, String title) {
        return playlistRepository.findByDiaryIdAndTitleContainingIgnoreCase(diaryId, title)
                .stream()
                .map(PlaylistDTO::fromEntity)
                .collect(Collectors.toList());
    }

    // playlist 생성
    public PlaylistDTO createPlaylist(PlaylistDTO playlistDTO) {
        // 중복 제목 확인
        if (playlistRepository.existsByTitleAndDiaryId(playlistDTO.getTitle(), playlistDTO.getDiaryId())) {
            throw new IllegalArgumentException("해당 다이어리에 동일한 제목의 플레이리스트가 이미 존재합니다.");
        }

        Playlist playlist = new Playlist(playlistDTO.getTitle(), playlistDTO.getDiaryId());
        Playlist savedPlaylist = playlistRepository.save(playlist);
        return PlaylistDTO.fromEntity(savedPlaylist);
    }

    // playlist 업데이트
    public PlaylistDTO updatePlaylist(Long playlistId, PlaylistDTO playlistDTO) {
        Playlist existingPlaylist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new IllegalArgumentException("해당 ID의 플레이리스트를 찾을 수 없습니다: " + playlistId));

        // 제목 변경 시 중복 확인
        if (!existingPlaylist.getTitle().equals(playlistDTO.getTitle())) {
            if (playlistRepository.existsByTitleAndDiaryId(playlistDTO.getTitle(), existingPlaylist.getDiaryId())) {
                throw new IllegalArgumentException("해당 다이어리에 동일한 제목의 플레이리스트가 이미 존재합니다.");
            }
        }

        existingPlaylist.setTitle(playlistDTO.getTitle());
        existingPlaylist.setDiaryId(playlistDTO.getDiaryId());

        Playlist updatedPlaylist = playlistRepository.save(existingPlaylist);
        return PlaylistDTO.fromEntity(updatedPlaylist);
    }

    // playlist 삭제
    public void deletePlaylist(Long playlistId) {
        if (!playlistRepository.existsById(playlistId)) {
            throw new IllegalArgumentException("해당 ID의 플레이리스트를 찾을 수 없습니다: " + playlistId);
        }
        playlistRepository.deleteById(playlistId);
    }

    // 특정 diary_id에 속한 playlist 개수 조회
    @Transactional(readOnly = true)
    public long countPlaylistsByDiaryId(Long diaryId) {
        return playlistRepository.countByDiaryId(diaryId);
    }

    // playlist 소유권 확인
    @Transactional(readOnly = true)
    public boolean isPlaylistOwnedByDiary(Long playlistId, Long diaryId) {
        return playlistRepository.findByPlaylistIdAndDiaryId(playlistId, diaryId).isPresent();
    }
} 