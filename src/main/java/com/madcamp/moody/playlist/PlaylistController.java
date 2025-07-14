package com.madcamp.moody.playlist;

import com.madcamp.moody.music.MusicDTO;
import com.madcamp.moody.music.MusicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/playlists")
@CrossOrigin(origins = "*")
public class PlaylistController {

    private final PlaylistService playlistService;
    private final MusicService musicService;

    @Autowired
    public PlaylistController(PlaylistService playlistService, MusicService musicService) {
        this.playlistService = playlistService;
        this.musicService = musicService;
    }

    // 모든 playlist 조회
    @GetMapping
    public ResponseEntity<List<PlaylistDTO>> getAllPlaylists() {
        List<PlaylistDTO> playlists = playlistService.getAllPlaylists();
        System.out.println("전체 플레이리스트 수: " + playlists.size());
        for (PlaylistDTO playlist : playlists) {
            System.out.println("플레이리스트: " + playlist.getTitle() + ", 사용자 ID: " + playlist.getDiaryId() + ", 날짜: " + playlist.getDate());
        }
        return ResponseEntity.ok(playlists);
    }

    // 사용자 ID와 날짜로 플레이리스트 조회 (음악 목록 포함) - 더 구체적인 패턴이므로 먼저 배치
    @GetMapping("/user/{userId}/date/{date}")
    public ResponseEntity<Map<String, Object>> getPlaylistsByUserAndDate(
            @PathVariable Long userId, 
            @PathVariable String date) {
        try {
            System.out.println("플레이리스트 조회 요청: 사용자 ID " + userId + ", 날짜 " + date);
            java.time.LocalDate localDate = java.time.LocalDate.parse(date);
            List<PlaylistDTO> playlists = playlistService.getPlaylistsByUserAndDate(userId, localDate);
            
            System.out.println("조회된 플레이리스트 수: " + playlists.size());
            for (PlaylistDTO playlist : playlists) {
                System.out.println("플레이리스트: " + playlist.getTitle() + ", 날짜: " + playlist.getDate() + ", ID: " + playlist.getPlaylistId());
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("playlists", playlists);
            
            // 플레이리스트가 있다면 첫 번째 플레이리스트의 음악 목록도 함께 반환
            if (!playlists.isEmpty()) {
                PlaylistDTO firstPlaylist = playlists.get(0);
                List<MusicDTO> musicList = musicService.getMusicByPlaylistId(firstPlaylist.getPlaylistId());
                System.out.println("첫 번째 플레이리스트의 음악 수: " + musicList.size());
                response.put("musicList", musicList);
                response.put("playlistTitle", firstPlaylist.getTitle());
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("플레이리스트 조회 오류: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
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

    // 새로운 playlist 생성 (같은 날짜에 기존 플레이리스트가 있으면 덮어씀)
    @PostMapping
    public ResponseEntity<PlaylistDTO> createPlaylist(@RequestBody PlaylistDTO playlistDTO) {
        try {
            PlaylistDTO createdPlaylist = playlistService.createOrUpdatePlaylist(playlistDTO);
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