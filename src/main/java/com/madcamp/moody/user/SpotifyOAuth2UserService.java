package com.madcamp.moody.user;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
public class SpotifyOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        try {
            OAuth2User oAuth2User = super.loadUser(userRequest);
            Map<String, Object> originalAttributes = oAuth2User.getAttributes();
            
            // 수정 가능한 새로운 Map 생성
            Map<String, Object> attributes = new HashMap<>(originalAttributes);
            
            // 스포티파이 액세스 토큰을 attributes에 추가
            String accessToken = userRequest.getAccessToken().getTokenValue();
            String refreshToken = null;
            attributes.put("access_token", accessToken);
            
            // 스포티파이 사용자 정보 파싱
            String spotifyId = String.valueOf(attributes.get("id"));
            String email = (String) attributes.get("email");
            String displayName = (String) attributes.get("display_name");
            
            System.out.println("=== Spotify OAuth2 사용자 정보 ===");
            System.out.println("Spotify ID: " + spotifyId);
            System.out.println("Display Name: " + displayName);
            System.out.println("Email: " + email);
            
            // 현재 SecurityContext에서 인증된 카카오 사용자 찾기
            Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
            User currentKakaoUser = null;
            
            if (currentAuth != null && currentAuth.isAuthenticated() && currentAuth.getPrincipal() instanceof OAuth2User) {
                OAuth2User currentOAuth2User = (OAuth2User) currentAuth.getPrincipal();
                
                // 현재 인증된 사용자가 카카오 사용자인지 확인 (display_name이 없으면 카카오)
                boolean isCurrentUserKakao = !currentOAuth2User.getAttributes().containsKey("display_name");
                
                if (isCurrentUserKakao) {
                    String kakaoOauthId = String.valueOf(currentOAuth2User.getAttributes().get("id"));
                    currentKakaoUser = userRepository.findByOauthId(kakaoOauthId);
                    
                    if (currentKakaoUser != null) {
                        System.out.println("현재 로그인한 카카오 사용자 발견: " + currentKakaoUser.getName() + " (ID: " + currentKakaoUser.getId() + ")");
                    }
                }
            }
            
            // 1. 기존에 이 Spotify 계정으로 로그인한 사용자가 있는지 확인
            User existingSpotifyUser = userRepository.findBySpotifyOauthId(spotifyId);
            
            if (existingSpotifyUser != null) {
                System.out.println("기존 Spotify 연동 사용자 발견: " + existingSpotifyUser.getName());
                // 토큰 업데이트
                existingSpotifyUser.setSpotifyAccessToken(accessToken);
                if (refreshToken != null) {
                    existingSpotifyUser.setSpotifyRefreshToken(refreshToken);
                }
                userRepository.save(existingSpotifyUser);
                
                return new DefaultOAuth2User(
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")),
                    attributes,
                    "id"
                );
            }
            
            // 2. 현재 로그인한 카카오 사용자가 있으면 Spotify 정보를 연동
            if (currentKakaoUser != null) {
                System.out.println("현재 카카오 사용자와 Spotify 연동 중...");
                
                // 카카오 사용자에 Spotify 정보 추가
                currentKakaoUser.setSpotifyOauthId(spotifyId);
                currentKakaoUser.setSpotifyEmail(email);
                currentKakaoUser.setSpotifyDisplayName(displayName);
                currentKakaoUser.setSpotifyAccessToken(accessToken);
                if (refreshToken != null) {
                    currentKakaoUser.setSpotifyRefreshToken(refreshToken);
                }
                
                userRepository.save(currentKakaoUser);
                System.out.println("현재 카카오 사용자와 Spotify 연동 완료: " + currentKakaoUser.getName());
                
                return new DefaultOAuth2User(
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")),
                    attributes,
                    "id"
                );
            }
            
            // 3. 이메일로 기존 카카오 사용자 찾기 (fallback)
            User userByEmail = null;
            if (email != null) {
                userByEmail = userRepository.findAll().stream()
                    .filter(u -> email.equals(u.getEmail()) && u.getSpotifyOauthId() == null)
                    .findFirst()
                    .orElse(null);
            }
            
            if (userByEmail != null) {
                System.out.println("이메일로 기존 사용자 발견, Spotify 정보 연동: " + userByEmail.getName());
                userByEmail.setSpotifyOauthId(spotifyId);
                userByEmail.setSpotifyEmail(email);
                userByEmail.setSpotifyDisplayName(displayName);
                userByEmail.setSpotifyAccessToken(accessToken);
                if (refreshToken != null) {
                    userByEmail.setSpotifyRefreshToken(refreshToken);
                }
                userRepository.save(userByEmail);
                
                return new DefaultOAuth2User(
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")),
                    attributes,
                    "id"
                );
            }
            
            // 4. 완전히 새로운 사용자 생성 (Spotify 전용 - 최후의 수단)
            System.out.println("새로운 Spotify 사용자 생성");
            User newUser = new User();
            newUser.setName(displayName != null ? displayName : "Spotify 사용자");
            newUser.setEmail(email);
            newUser.setOauthId(spotifyId); // 기본 OAuth ID는 Spotify ID로 설정
            newUser.setSpotifyOauthId(spotifyId);
            newUser.setSpotifyEmail(email);
            newUser.setSpotifyDisplayName(displayName);
            newUser.setSpotifyAccessToken(accessToken);
            if (refreshToken != null) {
                newUser.setSpotifyRefreshToken(refreshToken);
            }
            newUser.setOnboardingCompleted(false);
            
            userRepository.save(newUser);
            System.out.println("새로운 Spotify 사용자 생성 완료: " + newUser.getName());
            
            return new DefaultOAuth2User(
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                "id"
            );
            
        } catch (Exception e) {
            System.err.println("Spotify OAuth2 처리 중 오류: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
} 