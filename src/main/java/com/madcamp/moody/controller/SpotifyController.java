package com.madcamp.moody.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.madcamp.moody.spotify.SpotifyService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/spotify")
@CrossOrigin(origins = "http://127.0.0.1:3000", allowCredentials = "true")
public class SpotifyController {

    @Autowired
    private SpotifyService spotifyService;

    /**
     * Spotify Web Playback SDK용 액세스 토큰 제공
     * 카카오 인증과 완전히 분리된 Spotify 전용 API
     */
    @GetMapping("/access-token")
    public ResponseEntity<Map<String, Object>> getSpotifyAccessToken() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String accessToken = spotifyService.getCurrentUserSpotifyAccessToken();
            
            if (accessToken != null) {
                response.put("accessToken", accessToken);
                response.put("success", true);
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("error", "Spotify 액세스 토큰을 찾을 수 없습니다.");
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "액세스 토큰 조회 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Spotify 연동 상태 확인
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSpotifyStatus() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean isLinked = spotifyService.isCurrentUserSpotifyLinked();
            response.put("spotifyLinked", isLinked);
            response.put("success", true);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Spotify 상태 확인 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Spotify 재생 시작
     */
    @PostMapping("/play")
    public ResponseEntity<Map<String, Object>> startPlayback(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String deviceId = (String) request.get("device_id");
            boolean success = spotifyService.startSpotifyPlayback(deviceId);
            
            response.put("success", success);
            if (success) {
                response.put("message", "재생이 시작되었습니다.");
            } else {
                response.put("error", "재생 시작에 실패했습니다.");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "재생 시작 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Spotify 재생 제어 (재생/일시정지)
     */
    @PostMapping("/control")
    public ResponseEntity<Map<String, Object>> controlPlayback(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String action = (String) request.get("action"); // "play" 또는 "pause"
            String deviceId = (String) request.get("device_id");
            
            boolean success = spotifyService.controlSpotifyPlayback(action, deviceId);
            
            response.put("success", success);
            if (success) {
                response.put("message", action.equals("play") ? "재생되었습니다." : "일시정지되었습니다.");
            } else {
                response.put("error", "재생 제어에 실패했습니다.");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "재생 제어 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
} 