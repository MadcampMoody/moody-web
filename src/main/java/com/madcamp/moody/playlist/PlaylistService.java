package com.madcamp.moody.playlist;

import com.madcamp.moody.music.MusicRepository;
import com.madcamp.moody.music.MusicService;
import com.madcamp.moody.music.MusicDTO;
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
    private final MusicRepository musicRepository;
    private final MusicService musicService;

    @Autowired
    public PlaylistService(PlaylistRepository playlistRepository, MusicRepository musicRepository, MusicService musicService) {
        this.playlistRepository = playlistRepository;
        this.musicRepository = musicRepository;
        this.musicService = musicService;
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

        Playlist playlist = new Playlist(playlistDTO.getTitle(), playlistDTO.getDiaryId(), playlistDTO.getDate());
        Playlist savedPlaylist = playlistRepository.save(playlist);
        return PlaylistDTO.fromEntity(savedPlaylist);
    }

    // playlist 생성 또는 업데이트 (같은 날짜에 기존 플레이리스트가 있으면 덮어씀)
    public PlaylistDTO createOrUpdatePlaylist(PlaylistDTO playlistDTO) {
        System.out.println("createOrUpdatePlaylist 호출: diaryId=" + playlistDTO.getDiaryId() + ", date=" + playlistDTO.getDate());
        
        // 같은 날짜에 기존 플레이리스트가 있는지 확인
        List<Playlist> existingPlaylists = playlistRepository.findByDiaryIdAndDate(playlistDTO.getDiaryId(), playlistDTO.getDate());
        
        if (!existingPlaylists.isEmpty()) {
            System.out.println("기존 플레이리스트 발견: " + existingPlaylists.size() + "개");
            // 기존 플레이리스트가 있으면 모두 삭제 (음악도 함께 삭제됨)
            for (Playlist existingPlaylist : existingPlaylists) {
                System.out.println("기존 플레이리스트 삭제: playlistId=" + existingPlaylist.getPlaylistId());
                // 연관된 음악들 먼저 삭제
                musicRepository.deleteByPlaylistId(existingPlaylist.getPlaylistId());
                // 플레이리스트 삭제
                playlistRepository.deleteById(existingPlaylist.getPlaylistId());
            }
        }
        
        // 1. 새 플레이리스트 생성 및 저장
        Playlist playlist = new Playlist(playlistDTO.getTitle(), playlistDTO.getDiaryId(), playlistDTO.getDate());
        Playlist savedPlaylist = playlistRepository.save(playlist);
        System.out.println("새 플레이리스트 생성: playlistId=" + savedPlaylist.getPlaylistId());

        // 2. 연관된 음악들 저장
        List<MusicDTO> musicDTOs = playlistDTO.getMusics();
        if (musicDTOs != null && !musicDTOs.isEmpty()) {
            // 각 MusicDTO에 새로 생성된 playlistId를 설정
            musicDTOs.forEach(musicDTO -> musicDTO.setPlaylistId(savedPlaylist.getPlaylistId()));
            // MusicService를 통해 음악 목록 저장
            musicService.createMusics(musicDTOs);
            System.out.println(musicDTOs.size() + "개의 음악을 플레이리스트에 추가했습니다.");
        }
        
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
    
    // 사용자 ID와 날짜로 플레이리스트 조회
    @Transactional(readOnly = true)
    public List<PlaylistDTO> getPlaylistsByUserAndDate(Long userId, java.time.LocalDate date) {
        return playlistRepository.findByDiaryIdAndDate(userId, date)
                .stream()
                .map(PlaylistDTO::fromEntity)
                .collect(Collectors.toList());
    }
} 