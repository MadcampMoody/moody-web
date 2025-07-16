package com.madcamp.moody.controller;

import com.madcamp.moody.user.User;
import com.madcamp.moody.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://127.0.0.1:3000", allowCredentials = "true")
public class AuthController_spotify {
    @Autowired
    private UserRepository userRepository;

    @GetMapping("/spotify-status")
    public ResponseEntity<?> checkSpotifyAuth(@AuthenticationPrincipal OAuth2User oauth2User) {
        try {
            System.out.println("=== checkSpotifyAuth called ===");
            System.out.println("OAuth2User: " + (oauth2User != null ? "present" : "null"));
            if (oauth2User == null) {
                return ResponseEntity.ok(Map.of("spotifyLoggedIn", false));
            }
            String spotifyId = oauth2User.getAttribute("id");
            User user = userRepository.findByOauthId(spotifyId);
            if (user != null) {
                return ResponseEntity.ok(Map.of("spotifyLoggedIn", true));
            } else {
                return ResponseEntity.ok(Map.of("spotifyLoggedIn", false));
            }
        } catch (Exception e) {
            System.err.println("Error in checkSpotifyAuth: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "서버 오류가 발생했습니다"));
        }
    }
} 