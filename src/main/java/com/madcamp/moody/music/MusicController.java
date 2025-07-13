package com.madcamp.moody.music;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/music")
@CrossOrigin(origins = "*")
public class MusicController {

    private final MusicService musicService;

    @Autowired
    public MusicController(MusicService musicService) {
        this.musicService = musicService;
    }

    // 모든 music 조회
    @GetMapping
    public ResponseEntity<List<MusicDTO>> getAllMusic() {
        List<MusicDTO> musicList = musicService.getAllMusic();
        return ResponseEntity.ok(musicList);
    }

    // ID로 music 조회
    @GetMapping("/{id}")
    public ResponseEntity<MusicDTO> getMusicById(@PathVariable Long id) {
        Optional<MusicDTO> music = musicService.getMusicById(id);
        return music.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // playlist_id로 music 조회
    @GetMapping("/playlist/{playlistId}")
    public ResponseEntity<List<MusicDTO>> getMusicByPlaylistId(@PathVariable Long playlistId) {
        List<MusicDTO> musicList = musicService.getMusicByPlaylistId(playlistId);
        return ResponseEntity.ok(musicList);
    }

    // music_url로 music 검색
    @GetMapping("/search")
    public ResponseEntity<List<MusicDTO>> searchMusicByUrl(@RequestParam String url) {
        List<MusicDTO> musicList = musicService.searchMusicByUrl(url);
        return ResponseEntity.ok(musicList);
    }

    // playlist_id와 music_url로 music 검색
    @GetMapping("/search/{playlistId}")
    public ResponseEntity<List<MusicDTO>> searchMusicByPlaylistIdAndUrl(
            @PathVariable Long playlistId, 
            @RequestParam String url) {
        List<MusicDTO> musicList = musicService.searchMusicByPlaylistIdAndUrl(playlistId, url);
        return ResponseEntity.ok(musicList);
    }

    // 새로운 music 생성
    @PostMapping
    public ResponseEntity<MusicDTO> createMusic(@RequestBody MusicDTO musicDTO) {
        try {
            MusicDTO createdMusic = musicService.createMusic(musicDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdMusic);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // music 업데이트
    @PutMapping("/{id}")
    public ResponseEntity<MusicDTO> updateMusic(@PathVariable Long id, @RequestBody MusicDTO musicDTO) {
        try {
            MusicDTO updatedMusic = musicService.updateMusic(id, musicDTO);
            return ResponseEntity.ok(updatedMusic);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // music 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMusic(@PathVariable Long id) {
        try {
            musicService.deleteMusic(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // 특정 playlist_id에 속한 모든 music 삭제
    @DeleteMapping("/playlist/{playlistId}")
    public ResponseEntity<Void> deleteMusicByPlaylistId(@PathVariable Long playlistId) {
        musicService.deleteMusicByPlaylistId(playlistId);
        return ResponseEntity.noContent().build();
    }

    // 특정 playlist_id에 속한 music 개수 조회
    @GetMapping("/count/{playlistId}")
    public ResponseEntity<Long> countMusicByPlaylistId(@PathVariable Long playlistId) {
        long count = musicService.countMusicByPlaylistId(playlistId);
        return ResponseEntity.ok(count);
    }

    // music 소유권 확인
    @GetMapping("/{musicId}/owner/{playlistId}")
    public ResponseEntity<Boolean> isMusicOwnedByPlaylist(@PathVariable Long musicId, @PathVariable Long playlistId) {
        boolean isOwned = musicService.isMusicOwnedByPlaylist(musicId, playlistId);
        return ResponseEntity.ok(isOwned);
    }

    // 에러 처리
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
} 