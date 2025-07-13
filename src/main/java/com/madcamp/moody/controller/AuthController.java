package com.madcamp.moody.controller;

import com.madcamp.moody.user.User;
import com.madcamp.moody.user.UserRepository;
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

            // 온보딩 데이터 업데이트 (필요한 경우)
            String nickname = (String) onboardingData.get("nickname");
            if (nickname != null && !nickname.trim().isEmpty()) {
                user.setName(nickname);
            }
            
            userRepository.save(user);
            
            return ResponseEntity.ok(Map.of("message", "온보딩이 완료되었습니다"));
            
        } catch (Exception e) {
            System.err.println("Error in completeOnboarding: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "서버 오류가 발생했습니다"));
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