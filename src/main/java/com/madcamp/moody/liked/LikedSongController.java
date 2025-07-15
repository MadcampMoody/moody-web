package com.madcamp.moody.liked;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/liked-songs")
public class LikedSongController {

    private final LikedSongService likedSongService;

    @Autowired
    public LikedSongController(LikedSongService likedSongService) {
        this.likedSongService = likedSongService;
    }

    @PostMapping("/add")
    public ResponseEntity<LikedSongDTO> addLikedSong(@RequestBody LikedSongRequestDTO requestDTO, @AuthenticationPrincipal OAuth2User user) {
        LikedSongDTO likedSong = likedSongService.addLikedSong(requestDTO, user);
        return ResponseEntity.ok(likedSong);
    }
    
    @PostMapping("/remove")
    public ResponseEntity<Void> removeLikedSong(@RequestBody LikedSongRequestDTO requestDTO, @AuthenticationPrincipal OAuth2User user) {
        likedSongService.removeLikedSong(requestDTO, user);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<LikedSongDTO>> getLikedSongs(@AuthenticationPrincipal OAuth2User user) {
        if (user == null) {
            return ResponseEntity.status(401).build(); // Unauthorized
        }
        List<LikedSongDTO> likedSongs = likedSongService.getLikedSongs(user);
        return ResponseEntity.ok(likedSongs);
    }

    @GetMapping("/ids")
    public ResponseEntity<Set<String>> getLikedTrackIds(@AuthenticationPrincipal OAuth2User user) {
        if (user == null) {
            return ResponseEntity.ok(Set.of()); // 로그인 안 한 경우 빈 Set 반환
        }
        Set<String> trackIds = likedSongService.getLikedTrackIds(user);
        return ResponseEntity.ok(trackIds);
    }
} 