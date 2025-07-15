package com.madcamp.moody.user;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpSession;
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
            // Refresh token은 OAuth2AccessToken에서 직접 가져올 수 없으므로 null로 설정
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
            
            // 세션에서 현재 로그인된 카카오 사용자 찾기
            ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpSession session = attr.getRequest().getSession();
            
            // 1. 기존에 이 Spotify 계정으로 로그인한 사용자가 있는지 확인
            User existingUser = userRepository.findBySpotifyOauthId(spotifyId);
            
            if (existingUser != null) {
                System.out.println("기존 Spotify 연동 사용자 발견: " + existingUser.getName());
                // 토큰 업데이트
                existingUser.setSpotifyAccessToken(accessToken);
                if (refreshToken != null) {
                    existingUser.setSpotifyRefreshToken(refreshToken);
                }
                userRepository.save(existingUser);
                
                return new DefaultOAuth2User(
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")),
                    attributes,
                    "id"
                );
            }
            
            // 2. 세션에서 현재 인증된 카카오 사용자 정보 가져오기
            Object kakaoUserIdObj = session.getAttribute("kakaoUserId");
            if (kakaoUserIdObj != null) {
                Long kakaoUserId = (Long) kakaoUserIdObj;
                User kakaoUser = userRepository.findById(kakaoUserId).orElse(null);
                
                if (kakaoUser != null) {
                    System.out.println("카카오 사용자와 Spotify 연동: " + kakaoUser.getName());
                    
                    // 카카오 사용자에 Spotify 정보 추가
                    kakaoUser.setSpotifyOauthId(spotifyId);
                    kakaoUser.setSpotifyEmail(email);
                    kakaoUser.setSpotifyDisplayName(displayName);
                    kakaoUser.setSpotifyAccessToken(accessToken);
                    if (refreshToken != null) {
                        kakaoUser.setSpotifyRefreshToken(refreshToken);
                    }
                    
                    userRepository.save(kakaoUser);
                    System.out.println("Spotify 정보가 카카오 사용자에 연동 완료");
                    
                    return new DefaultOAuth2User(
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")),
                        attributes,
                        "id"
                    );
                }
            }
            
            // 3. 세션에 카카오 사용자 정보가 없으면 Spotify 단독 로그인으로 처리
            System.out.println("Spotify 단독 로그인 시도");
            
            // 이메일로 기존 사용자 찾기
            User userByEmail = null;
            if (email != null) {
                // 이메일로 기존 사용자 찾기 (Kakao 사용자 중에서)
                userByEmail = userRepository.findAll().stream()
                    .filter(u -> email.equals(u.getEmail()) && u.getSpotifyOauthId() == null)
                    .findFirst()
                    .orElse(null);
            }
            
            if (userByEmail != null) {
                System.out.println("이메일로 기존 사용자 발견, Spotify 정보 연동: " + userByEmail.getName());
                // 기존 사용자에 Spotify 정보 추가
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
            
            // 4. 완전히 새로운 사용자 생성 (Spotify 기반)
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