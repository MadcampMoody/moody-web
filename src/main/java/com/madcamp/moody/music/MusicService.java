package com.madcamp.moody.music;

import com.madcamp.moody.user.User;
import com.madcamp.moody.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class MusicService {

    private final MusicRepository musicRepository;
    private final UserRepository userRepository;

    @Autowired
    public MusicService(MusicRepository musicRepository, UserRepository userRepository) {
        this.musicRepository = musicRepository;
        this.userRepository = userRepository;
    }

    // 모든 music 조회
    @Transactional(readOnly = true)
    public List<MusicDTO> getAllMusic() {
        return musicRepository.findAll()
                .stream()
                .map(MusicDTO::fromEntity)
                .collect(Collectors.toList());
    }

    // ID로 music 조회
    @Transactional(readOnly = true)
    public Optional<MusicDTO> getMusicById(Long musicId) {
        return musicRepository.findById(musicId)
                .map(MusicDTO::fromEntity);
    }

    // playlist_id로 music 조회
    @Transactional(readOnly = true)
    public List<MusicDTO> getMusicByPlaylistId(Long playlistId) {
        return musicRepository.findByPlaylistIdOrderByMusicIdAsc(playlistId)
                .stream()
                .map(MusicDTO::fromEntity)
                .collect(Collectors.toList());
    }

    // music_url로 music 검색
    @Transactional(readOnly = true)
    public List<MusicDTO> searchMusicByUrl(String musicUrl) {
        return musicRepository.findByMusicUrlContainingIgnoreCase(musicUrl)
                .stream()
                .map(MusicDTO::fromEntity)
                .collect(Collectors.toList());
    }

    // playlist_id와 music_url로 music 검색
    @Transactional(readOnly = true)
    public List<MusicDTO> searchMusicByPlaylistIdAndUrl(Long playlistId, String musicUrl) {
        return musicRepository.findByPlaylistIdAndMusicUrlContainingIgnoreCase(playlistId, musicUrl)
                .stream()
                .map(MusicDTO::fromEntity)
                .collect(Collectors.toList());
    }

    // music 생성
    public MusicDTO createMusic(MusicDTO musicDTO) {
        // 중복 URL 확인 (같은 플레이리스트 내에서)
        if (musicRepository.existsByMusicUrlAndPlaylistId(musicDTO.getMusicUrl(), musicDTO.getPlaylistId())) {
            throw new IllegalArgumentException("해당 플레이리스트에 동일한 URL의 음악이 이미 존재합니다.");
        }

        Music music = new Music(musicDTO.getMusicUrl(), musicDTO.getPlaylistId());
        Music savedMusic = musicRepository.save(music);
        return MusicDTO.fromEntity(savedMusic);
    }

    // 여러 music 생성
    public List<MusicDTO> createMusics(List<MusicDTO> musicDTOs) {
        List<Music> musicList = musicDTOs.stream()
                .map(dto -> {
                    Music music = new Music(dto.getMusicUrl(), dto.getPlaylistId());
                    if (dto.getUserId() != null) {
                        User user = userRepository.findById(dto.getUserId())
                                .orElseThrow(() -> new IllegalArgumentException("Invalid user Id:" + dto.getUserId()));
                        music.setUser(user);
                    }
                    return music;
                })
                .collect(Collectors.toList());
        
        List<Music> savedMusicList = musicRepository.saveAll(musicList);
        
        return savedMusicList.stream()
                .map(MusicDTO::fromEntity)
                .collect(Collectors.toList());
    }

    // music 업데이트
    public MusicDTO updateMusic(Long musicId, MusicDTO musicDTO) {
        Music existingMusic = musicRepository.findById(musicId)
                .orElseThrow(() -> new IllegalArgumentException("해당 ID의 음악을 찾을 수 없습니다: " + musicId));

        // URL 변경 시 중복 확인
        if (!existingMusic.getMusicUrl().equals(musicDTO.getMusicUrl())) {
            if (musicRepository.existsByMusicUrlAndPlaylistId(musicDTO.getMusicUrl(), existingMusic.getPlaylistId())) {
                throw new IllegalArgumentException("해당 플레이리스트에 동일한 URL의 음악이 이미 존재합니다.");
            }
        }

        existingMusic.setMusicUrl(musicDTO.getMusicUrl());
        existingMusic.setPlaylistId(musicDTO.getPlaylistId());

        Music updatedMusic = musicRepository.save(existingMusic);
        return MusicDTO.fromEntity(updatedMusic);
    }

    // music 삭제
    public void deleteMusic(Long musicId) {
        if (!musicRepository.existsById(musicId)) {
            throw new IllegalArgumentException("해당 ID의 음악을 찾을 수 없습니다: " + musicId);
        }
        musicRepository.deleteById(musicId);
    }

    // 특정 playlist_id에 속한 모든 music 삭제
    public void deleteMusicByPlaylistId(Long playlistId) {
        musicRepository.deleteByPlaylistId(playlistId);
    }

    // 특정 playlist_id에 속한 music 개수 조회
    @Transactional(readOnly = true)
    public long countMusicByPlaylistId(Long playlistId) {
        return musicRepository.countByPlaylistId(playlistId);
    }

    // music 소유권 확인
    @Transactional(readOnly = true)
    public boolean isMusicOwnedByPlaylist(Long musicId, Long playlistId) {
        return musicRepository.findByMusicIdAndPlaylistId(musicId, playlistId).isPresent();
    }
} 