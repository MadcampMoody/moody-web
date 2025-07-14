package com.madcamp.moody.controller;

import com.madcamp.moody.music.MusicGenre;
import com.madcamp.moody.music.MusicRegion;
import com.madcamp.moody.user.User;
import com.madcamp.moody.user.UserRepository;

import java.util.List;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/user")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal OAuth2User oauth2User) {
        try {
            System.out.println("getCurrentUser called - OAuth2User: " + (oauth2User != null ? "present" : "null"));
            
            if (oauth2User == null) {
                System.out.println("OAuth2User is null, returning 401");
                return ResponseEntity.status(401).body(Map.of("error", "인증되지 않은 사용자"));
            }

            // OAuth2User의 id 속성을 안전하게 처리
            Object idAttribute = oauth2User.getAttribute("id");
            String oauthId;
            
            if (idAttribute instanceof Long) {
                oauthId = String.valueOf((Long) idAttribute);
            } else if (idAttribute instanceof String) {
                oauthId = (String) idAttribute;
            } else {
                oauthId = String.valueOf(idAttribute);
            }
            
            System.out.println("OAuth ID: " + oauthId);
            
            User user = userRepository.findByOauthId(oauthId);
            System.out.println("User from DB: " + (user != null ? "found" : "not found"));

            if (user == null) {
                System.out.println("User not found in database, returning 404");
                return ResponseEntity.status(404).body(Map.of("error", "사용자를 찾을 수 없습니다"));
            }

            // 신규/기존 회원 판단 (createdAt 기준으로 판단)
            boolean isNewUser = false;
            if (user.getCreatedAt() != null) {
                // 현재 시간과 생성 시간의 차이가 30초 이내면 신규 회원으로 간주
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime createdAt = user.getCreatedAt();
                long secondsDiff = java.time.Duration.between(createdAt, now).getSeconds();
                isNewUser = secondsDiff <= 30; // 30초 이내에 생성된 사용자는 신규 회원
            }

            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", user.getId());
            userInfo.put("name", user.getName());
            userInfo.put("email", user.getEmail());
            userInfo.put("oauthId", user.getOauthId());
            userInfo.put("isNewUser", isNewUser);

            System.out.println("Returning user info: " + userInfo);
            return ResponseEntity.ok(userInfo);
            
        } catch (Exception e) {
            System.err.println("Error in getCurrentUser: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "서버 오류가 발생했습니다: " + e.getMessage()));
        }
    }



    @GetMapping("/check")
    public ResponseEntity<?> checkAuth(@AuthenticationPrincipal OAuth2User oauth2User) {
        try {
            System.out.println("checkAuth called - OAuth2User: " + (oauth2User != null ? "present" : "null"));
            
            if (oauth2User == null) {
                System.out.println("User is not authenticated");
                return ResponseEntity.status(401).body(Map.of("authenticated", false));
            }
            System.out.println("User is authenticated");
            return ResponseEntity.ok(Map.of("authenticated", true));
        } catch (Exception e) {
            System.err.println("Error in checkAuth: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "서버 오류가 발생했습니다"));
        }
    }

    // OAuth2 성공 콜백 처리
    @GetMapping("/success")
    public ResponseEntity<?> oauth2Success(@AuthenticationPrincipal OAuth2User oauth2User, HttpServletRequest request) {
        try {
            System.out.println("OAuth2 Success callback called");
            System.out.println("OAuth2User: " + (oauth2User != null ? "present" : "null"));
            
            if (oauth2User == null) {
                System.out.println("OAuth2User is null, redirecting to login with error");
                return ResponseEntity.status(302)
                    .header("Location", "http://localhost:3000/?error=true")
                    .build();
            }
            
            // OAuth2User의 id 속성을 안전하게 처리
            Object idAttribute = oauth2User.getAttribute("id");
            String oauthId;
            
            if (idAttribute instanceof Long) {
                oauthId = String.valueOf((Long) idAttribute);
            } else if (idAttribute instanceof String) {
                oauthId = (String) idAttribute;
            } else {
                oauthId = String.valueOf(idAttribute);
            }
            
            System.out.println("OAuth ID in success: " + oauthId);
            
            User user = userRepository.findByOauthId(oauthId);
            System.out.println("User from DB in success: " + (user != null ? "found" : "not found"));
            
            if (user == null) {
                System.out.println("User not found in database, redirecting to login with error");
                return ResponseEntity.status(302)
                    .header("Location", "http://localhost:3000/?error=true")
                    .build();
            }
            
            // 카카오 액세스 토큰 추출 (OAuth2User attributes에서)
            String accessToken = null;
            if (oauth2User.getAttribute("access_token") != null) {
                accessToken = oauth2User.getAttribute("access_token");
            }
            
            System.out.println("Authentication successful, redirecting to dashboard");
            // 성공 시 React 앱의 대시보드로 리다이렉트 (토큰을 URL 파라미터로 전달)
            String redirectUrl = "http://localhost:3000/dashboard";
            if (accessToken != null) {
                redirectUrl += "?token=" + accessToken;
            }
            
            return ResponseEntity.status(302)
                .header("Location", redirectUrl)
                .build();
                
        } catch (Exception e) {
            System.err.println("Error in oauth2Success: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(302)
                .header("Location", "http://localhost:3000/?error=true")
                .build();
        }
    }

    // OAuth2 실패 콜백 처리
    @GetMapping("/failure")
    public ResponseEntity<?> oauth2Failure() {
        System.out.println("OAuth2 Failure callback called");
        // 실패 시 React 앱의 로그인 페이지로 리다이렉트
        return ResponseEntity.status(302)
            .header("Location", "http://localhost:3000/?error=true")
            .build();
    }

    // 온보딩 완료 처리
    @PostMapping("/onboarding-complete")
    public ResponseEntity<?> completeOnboarding(@AuthenticationPrincipal OAuth2User oauth2User, 
                                               @RequestBody Map<String, Object> onboardingData) {
        try {
            if (oauth2User == null) {
                return ResponseEntity.status(401).body(Map.of("error", "인증되지 않은 사용자"));
            }

            Object idAttribute = oauth2User.getAttribute("id");
            String oauthId;
            
            if (idAttribute instanceof Long) {
                oauthId = String.valueOf((Long) idAttribute);
            } else if (idAttribute instanceof String) {
                oauthId = (String) idAttribute;
            } else {
                oauthId = String.valueOf(idAttribute);
            }
            
            User user = userRepository.findByOauthId(oauthId);
            if (user == null) {
                return ResponseEntity.status(404).body(Map.of("error", "사용자를 찾을 수 없습니다"));
            }

            // 온보딩 데이터 업데이트
            String nickname = (String) onboardingData.get("nickname");
            if (nickname != null && !nickname.trim().isEmpty()) {
                user.setName(nickname);
            }
            
            // 음악 지역 선호도 저장
            String musicRegion = (String) onboardingData.get("musicRegion");
            if (musicRegion != null) {
                user.setMusicRegion(MusicRegion.fromValue(musicRegion));
            }
            
            // 음악 장르 선호도 저장 (여러 장르를 JSON으로 저장)
            @SuppressWarnings("unchecked")
            List<String> musicPreferences = (List<String>) onboardingData.get("musicPreferences");
            if (musicPreferences != null && !musicPreferences.isEmpty()) {
                try {
                    // 프론트엔드 용어를 데이터베이스 용어로 변환
                    List<String> convertedGenres = new ArrayList<>();
                    for (String genre : musicPreferences) {
                        MusicGenre musicGenre = convertToMusicGenre(genre);
                        if (musicGenre != null) {
                            convertedGenres.add(musicGenre.name());
                        }
                    }
                    
                    ObjectMapper objectMapper = new ObjectMapper();
                    String musicGenresJson = objectMapper.writeValueAsString(convertedGenres);
                    user.setMusicGenres(musicGenresJson);
                } catch (JsonProcessingException e) {
                    System.err.println("Error serializing music genres: " + e.getMessage());
                }
            }
            
            userRepository.save(user);
            
            return ResponseEntity.ok(Map.of("message", "온보딩이 완료되었습니다"));
            
        } catch (Exception e) {
            System.err.println("Error in completeOnboarding: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "서버 오류가 발생했습니다"));
        }
    }

    // 프론트엔드 용어를 MusicGenre enum으로 변환하는 메서드
    private MusicGenre convertToMusicGenre(String frontendGenre) {
        switch (frontendGenre) {
            case "팝": return MusicGenre.pop;
            case "락": return MusicGenre.rock;
            case "힙합": return MusicGenre.hip_hop;
            case "R&B": return MusicGenre.r_n_b;
            case "K-POP": return MusicGenre.k_pop;
            case "재즈": return MusicGenre.jazz;
            case "EDM": return MusicGenre.electronic;
            case "컨트리": return MusicGenre.country;
            case "댄스": return MusicGenre.dance;
            case "인디": return MusicGenre.indie;
            default: return null;
        }
    }

    // 로그아웃 성공 콜백 처리
    @GetMapping("/logout-success")
    public ResponseEntity<?> logoutSuccess() {
        // 로그아웃 시 React 앱의 로그인 페이지로 리다이렉트
        return ResponseEntity.status(302)
            .header("Location", "http://localhost:3000/")
            .build();
    }
}