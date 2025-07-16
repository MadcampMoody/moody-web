package com.madcamp.moody.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.madcamp.moody.spotify.SpotifyService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.ServletException;
import java.io.IOException;
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
     * Spotify 전용 API
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

    /**
     * Spotify OAuth2 콜백에서 spotifyUserId를 세션에 저장
     * (Spring Security가 처리하기 전에 세션에 spotifyUserId를 넣어주기 위함)
     */
    @GetMapping("/callback")
    public void spotifyCallback(HttpServletRequest request, HttpServletResponse response, HttpSession session) throws IOException, ServletException {
        // 기존: String kakaoUserId = request.getParameter("kakaoUserId");
        // 수정: 스포티파이 id를 읽어서 저장
        String spotifyUserId = request.getParameter("spotifyUserId");
        if (spotifyUserId == null) {
            // 혹시 파라미터명이 id로 오는 경우도 대비
            spotifyUserId = request.getParameter("id");
        }
        if (spotifyUserId != null) {
            session.setAttribute("spotifyUserId", spotifyUserId);
            System.out.println("callback에서 spotifyUserId 세션 저장: " + spotifyUserId);
        }
        // Spring Security 기본 콜백 URL로 리다이렉트
        String code = request.getParameter("code");
        String state = request.getParameter("state");
        String redirectUrl = "/login/oauth2/code/spotify";
        if (code != null) {
            redirectUrl += "?code=" + code;
            if (state != null) {
                redirectUrl += "&state=" + state;
            }
        }
        request.getRequestDispatcher(redirectUrl).forward(request, response);
    }
} 