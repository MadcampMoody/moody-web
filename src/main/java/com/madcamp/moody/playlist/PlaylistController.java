package com.madcamp.moody.playlist;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/playlists")
@CrossOrigin(origins = "*")
public class PlaylistController {

    private final PlaylistService playlistService;

    @Autowired
    public PlaylistController(PlaylistService playlistService) {
        this.playlistService = playlistService;
    }

    // 모든 playlist 조회
    @GetMapping
    public ResponseEntity<List<PlaylistDTO>> getAllPlaylists() {
        List<PlaylistDTO> playlists = playlistService.getAllPlaylists();
        return ResponseEntity.ok(playlists);
    }

    // ID로 playlist 조회
    @GetMapping("/{id}")
    public ResponseEntity<PlaylistDTO> getPlaylistById(@PathVariable Long id) {
        Optional<PlaylistDTO> playlist = playlistService.getPlaylistById(id);
        return playlist.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // diary_id로 playlist 조회
    @GetMapping("/diary/{diaryId}")
    public ResponseEntity<List<PlaylistDTO>> getPlaylistsByDiaryId(@PathVariable Long diaryId) {
        List<PlaylistDTO> playlists = playlistService.getPlaylistsByDiaryId(diaryId);
        return ResponseEntity.ok(playlists);
    }

    // title로 playlist 검색
    @GetMapping("/search")
    public ResponseEntity<List<PlaylistDTO>> searchPlaylistsByTitle(@RequestParam String title) {
        List<PlaylistDTO> playlists = playlistService.searchPlaylistsByTitle(title);
        return ResponseEntity.ok(playlists);
    }

    // diary_id와 title로 playlist 검색
    @GetMapping("/search/{diaryId}")
    public ResponseEntity<List<PlaylistDTO>> searchPlaylistsByDiaryIdAndTitle(
            @PathVariable Long diaryId, 
            @RequestParam String title) {
        List<PlaylistDTO> playlists = playlistService.searchPlaylistsByDiaryIdAndTitle(diaryId, title);
        return ResponseEntity.ok(playlists);
    }

    // 새로운 playlist 생성
    @PostMapping
    public ResponseEntity<PlaylistDTO> createPlaylist(@RequestBody PlaylistDTO playlistDTO) {
        try {
            PlaylistDTO createdPlaylist = playlistService.createPlaylist(playlistDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdPlaylist);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // playlist 업데이트
    @PutMapping("/{id}")
    public ResponseEntity<PlaylistDTO> updatePlaylist(@PathVariable Long id, @RequestBody PlaylistDTO playlistDTO) {
        try {
            PlaylistDTO updatedPlaylist = playlistService.updatePlaylist(id, playlistDTO);
            return ResponseEntity.ok(updatedPlaylist);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // playlist 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlaylist(@PathVariable Long id) {
        try {
            playlistService.deletePlaylist(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // 특정 diary_id에 속한 playlist 개수 조회
    @GetMapping("/count/{diaryId}")
    public ResponseEntity<Long> countPlaylistsByDiaryId(@PathVariable Long diaryId) {
        long count = playlistService.countPlaylistsByDiaryId(diaryId);
        return ResponseEntity.ok(count);
    }

    // playlist 소유권 확인
    @GetMapping("/{playlistId}/owner/{diaryId}")
    public ResponseEntity<Boolean> isPlaylistOwnedByDiary(@PathVariable Long playlistId, @PathVariable Long diaryId) {
        boolean isOwned = playlistService.isPlaylistOwnedByDiary(playlistId, diaryId);
        return ResponseEntity.ok(isOwned);
    }

    // 에러 처리
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
} 