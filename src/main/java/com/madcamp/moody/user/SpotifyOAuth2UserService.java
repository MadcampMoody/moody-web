package com.madcamp.moody.user;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.Collections;
import java.util.Map;
import java.util.List;

@Service
public class SpotifyOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        try {
            OAuth2User oAuth2User = super.loadUser(userRequest);
            Map<String, Object> originalAttributes = oAuth2User.getAttributes();
            
            // Spotify 정보 파싱
            String spotifyId = String.valueOf(originalAttributes.get("id"));
            String email = (String) originalAttributes.get("email");
            String displayName = (String) originalAttributes.get("display_name");
            String accessToken = userRequest.getAccessToken().getTokenValue();
            String refreshToken = null; // 필요시 확장

            System.out.println("=== Spotify OAuth2 사용자 정보 ===");
            System.out.println("Spotify ID: " + spotifyId);
            System.out.println("Display Name: " + displayName);
            System.out.println("Email: " + email);

            // 본인 계정만 업데이트 (모든 유저 for문 삭제)
            User user = userRepository.findByOauthId(spotifyId);
            if (user == null) {
                // 신규 회원이면 생성
                user = new User();
                user.setOauthId(spotifyId);
                user.setEmail(email);
                user.setName(displayName);
            }
            user.setSpotifyOauthId(spotifyId);
            user.setSpotifyEmail(email);
            user.setSpotifyDisplayName(displayName);
            user.setSpotifyAccessToken(accessToken);
            if (refreshToken != null) {
                user.setSpotifyRefreshToken(refreshToken);
            }
            userRepository.save(user);
            System.out.println("Spotify 정보가 user_id=" + user.getId() + "에 연동 완료");

            // 기존 user로 계속 서비스 이용 (리다이렉트는 SuccessHandler에서 처리)
            return new DefaultOAuth2User(
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")),
                originalAttributes,
                "id"
            );
        } catch (Exception e) {
            System.err.println("Spotify OAuth2 처리 중 오류: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
} 