package com.madcamp.moody.controller;

import com.madcamp.moody.music.MusicGenre;
import com.madcamp.moody.music.MusicRegion;
import com.madcamp.moody.user.User;
import com.madcamp.moody.user.UserRepository;

import java.util.List;
import java.util.ArrayList;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.transaction.annotation.Transactional;
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
@CrossOrigin(origins = "http://127.0.0.1:3000", allowCredentials = "true")
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

            // 세션에 kakaoUserId가 없으면 oauth2User에서 id를 읽어 세션에 저장
            // 카카오 관련 코드/주석/메서드/엔드포인트 완전 삭제

            // 세션에서 kakaoUserId로 사용자 찾기
            User user = getCurrentAuthenticatedUser(oauth2User);
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
            userInfo.put("onboardingCompleted", user.isOnboardingCompleted());

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
    
    /**
     * 현재 인증된 사용자 정보 가져오기
     */
    private User getCurrentAuthenticatedUser(OAuth2User oauth2User) {
        try {
            System.out.println("=== getCurrentAuthenticatedUser (session 기반) called ===");
            Object idAttribute = oauth2User.getAttribute("id");
            if (idAttribute != null) {
                String oauthId = String.valueOf(idAttribute);
                User user = userRepository.findByOauthId(oauthId);
                if (user != null) return user;
            }
            System.out.println("세션에 kakaoUserId 없음");
            return null;
        } catch (Exception e) {
            System.err.println("현재 사용자 조회 실패: " + e.getMessage());
            e.printStackTrace();
            return null;
        } finally {
            System.out.println("=== getCurrentAuthenticatedUser ended ===");
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
                    .header("Location", "http://127.0.0.1:3000/?error=true")
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
            
            // getCurrentAuthenticatedUser 메서드를 사용해서 사용자 찾기
            User user = getCurrentAuthenticatedUser(oauth2User);
            System.out.println("User from DB in success: " + (user != null ? "found" : "not found"));
            
            if (user == null) {
                System.out.println("User not found in database, redirecting to login with error");
                return ResponseEntity.status(302)
                    .header("Location", "http://127.0.0.1:3000/?error=true")
                    .build();
            }
            
            // 카카오 액세스 토큰 추출 (OAuth2User attributes에서)
            String accessToken = null;
            if (oauth2User.getAttribute("access_token") != null) {
                accessToken = oauth2User.getAttribute("access_token");
            }
            
            System.out.println("Authentication successful, redirecting to dashboard");
            // 성공 시 React 앱의 대시보드로 리다이렉트 (토큰을 URL 파라미터로 전달)
            String redirectUrl = "http://127.0.0.1:3000/dashboard";
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
                .header("Location", "http://127.0.0.1:3000/?error=true")
                .build();
        }
    }

    // OAuth2 실패 콜백 처리
    @GetMapping("/failure")
    public ResponseEntity<?> oauth2Failure() {
        System.out.println("OAuth2 Failure callback called");
        // 실패 시 React 앱의 로그인 페이지로 리다이렉트
        return ResponseEntity.status(302)
            .header("Location", "http://127.0.0.1:3000/?error=true")
            .build();
    }

    // 온보딩 완료 처리
    @PostMapping("/onboarding-complete")
    @Transactional
    public ResponseEntity<?> completeOnboarding(@AuthenticationPrincipal OAuth2User oauth2User, 
                                               @RequestBody Map<String, Object> onboardingData,
                                               HttpServletRequest request) {
        try {
            System.out.println("=== 온보딩 완료 처리 시작 ===");
            System.out.println("받은 온보딩 데이터: " + onboardingData);
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
            
            // DB에서 user를 찾는 것은 오직 oauthId로만 한다 (세션/메서드 사용 X)
            User user = userRepository.findByOauthId(oauthId);
            if (user == null) {
                return ResponseEntity.status(404).body(Map.of("error", "사용자를 찾을 수 없습니다"));
            }
            
            System.out.println("사용자 찾음: " + user.getName() + " (ID: " + user.getId() + ")");

            // 온보딩 완료 표시
            user.setOnboardingCompleted(true);
            
            // 온보딩 데이터 업데이트
            String nickname = (String) onboardingData.get("nickname");
            System.out.println("닉네임: " + nickname);
            if (nickname != null && !nickname.trim().isEmpty()) {
                user.setName(nickname);
                System.out.println("닉네임 설정됨: " + nickname);
            }
            
            // 음악 지역 선호도 저장 (배열로 받아서 처리)
            @SuppressWarnings("unchecked")
            List<String> musicRegions = (List<String>) onboardingData.get("musicRegion");
            System.out.println("음악 지역: " + musicRegions);
            if (musicRegions != null && !musicRegions.isEmpty()) {
                // 여러 지역이 선택된 경우 BOTH로 설정, 하나만 선택된 경우 해당 지역으로 설정
                if (musicRegions.size() > 1) {
                    user.setMusicRegion(MusicRegion.BOTH);
                    System.out.println("음악 지역 설정됨: BOTH");
                } else {
                    String region = musicRegions.get(0);
                    MusicRegion musicRegion = MusicRegion.fromValue(region);
                    if (musicRegion != null) {
                        user.setMusicRegion(musicRegion);
                        System.out.println("음악 지역 설정됨: " + musicRegion);
                    }
                }
            }
            
            // 음악 장르 선호도 저장 (여러 장르를 JSON으로 저장)
            @SuppressWarnings("unchecked")
            List<String> musicPreferences = (List<String>) onboardingData.get("musicPreferences");
            System.out.println("음악 장르: " + musicPreferences);
            if (musicPreferences != null && !musicPreferences.isEmpty()) {
                try {
                    // 프론트엔드 용어를 데이터베이스 용어로 변환
                    List<String> convertedGenres = new ArrayList<>();
                    for (String genre : musicPreferences) {
                        MusicGenre musicGenre = MusicGenre.convertToMusicGenre(genre);
                        if (musicGenre != null) {
                            convertedGenres.add(musicGenre.name());
                        }
                    }
                    
                    ObjectMapper objectMapper = new ObjectMapper();
                    String musicGenresJson = objectMapper.writeValueAsString(convertedGenres);
                    user.setMusicGenres(musicGenresJson);
                    System.out.println("음악 장르 설정됨: " + musicGenresJson);
                } catch (JsonProcessingException e) {
                    System.err.println("Error serializing music genres: " + e.getMessage());
                }
            }
            
            System.out.println("저장 전 사용자 정보 - 이름: " + user.getName() + ", 지역: " + user.getMusicRegion() + ", 장르: " + user.getMusicGenres() + ", 온보딩완료: " + user.isOnboardingCompleted());
            
            User savedUser = userRepository.save(user);
            System.out.println("save() 메서드 반환값 - 이름: " + savedUser.getName() + ", 지역: " + savedUser.getMusicRegion() + ", 장르: " + savedUser.getMusicGenres() + ", 온보딩완료: " + savedUser.isOnboardingCompleted());
            
            userRepository.flush(); // 명시적으로 flush 호출
            System.out.println("flush() 완료");
            
            System.out.println("사용자 정보 저장 완료");
            System.out.println("저장 후 사용자 정보 - 이름: " + user.getName() + ", 지역: " + user.getMusicRegion() + ", 장르: " + user.getMusicGenres() + ", 온보딩완료: " + user.isOnboardingCompleted());
            
            // 저장 후 다시 조회해서 확인
            User retrievedUser = userRepository.findByOauthId(oauthId);
            if (retrievedUser != null) {
                System.out.println("재조회된 사용자 정보 - 이름: " + retrievedUser.getName() + ", 지역: " + retrievedUser.getMusicRegion() + ", 장르: " + retrievedUser.getMusicGenres() + ", 온보딩완료: " + retrievedUser.isOnboardingCompleted());
            }
            
            // 업데이트된 사용자 정보를 응답에 포함 (재조회된 정보 사용)
            Map<String, Object> response = new HashMap<>();
            response.put("message", "온보딩이 완료되었습니다");
            
            User responseUser = retrievedUser != null ? retrievedUser : user;
            response.put("user", Map.of(
                "id", responseUser.getId(),
                "name", responseUser.getName(),
                "onboardingCompleted", responseUser.isOnboardingCompleted(),
                "musicRegion", responseUser.getMusicRegion(),
                "musicGenres", responseUser.getMusicGenres()
            ));
            
            // 온보딩 완료 후 세션에 kakaoUserId 저장
            // 카카오 관련 코드/주석/메서드/엔드포인트 완전 삭제

            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("Error in completeOnboarding: " + e.getMessage());
            System.err.println("Exception type: " + e.getClass().getSimpleName());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "서버 오류가 발생했습니다: " + e.getMessage()));
        }
    }



    // 디버깅용: 현재 사용자 정보 확인
    @GetMapping("/debug-user")
    public ResponseEntity<?> debugUser(@AuthenticationPrincipal OAuth2User oauth2User) {
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
            
            // getCurrentAuthenticatedUser 메서드를 사용해서 사용자 찾기
            User user = getCurrentAuthenticatedUser(oauth2User);
            if (user == null) {
                return ResponseEntity.status(404).body(Map.of("error", "사용자를 찾을 수 없습니다"));
            }
            
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", user.getId());
            userInfo.put("name", user.getName());
            userInfo.put("oauthId", user.getOauthId());
            userInfo.put("onboardingCompleted", user.isOnboardingCompleted());
            userInfo.put("musicRegion", user.getMusicRegion());
            userInfo.put("musicGenres", user.getMusicGenres());
            userInfo.put("createdAt", user.getCreatedAt());
            userInfo.put("updatedAt", user.getUpdatedAt());
            
            return ResponseEntity.ok(userInfo);
            
        } catch (Exception e) {
            System.err.println("Error in debugUser: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "서버 오류가 발생했습니다"));
        }
    }

    // 로그아웃 성공 콜백 처리
    @GetMapping("/logout-success")
    public ResponseEntity<?> logoutSuccess() {
        // 로그아웃 시 React 앱의 로그인 페이지로 리다이렉트
        return ResponseEntity.status(302)
            .header("Location", "http://127.0.0.1:3000/")
            .build();
    }

    /**
     * 카카오 유저 ID를 세션에 저장하는 엔드포인트
     */
    @PostMapping("/set-kakao-session")
    public ResponseEntity<?> setKakaoSession(@RequestBody Map<String, Object> body, HttpSession session) {
        Object kakaoUserId = body.get("kakaoUserId");
        if (kakaoUserId != null) {
            session.setAttribute("kakaoUserId", Long.valueOf(kakaoUserId.toString()));
            System.out.println("set-kakao-session: 세션에 kakaoUserId 저장: " + kakaoUserId);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().body("kakaoUserId is required");
    }
}