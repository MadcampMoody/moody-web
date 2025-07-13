package com.madcamp.moody.user;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        try {
            OAuth2User oAuth2User = super.loadUser(userRequest);
            Map<String, Object> originalAttributes = oAuth2User.getAttributes();
            
            // 수정 가능한 새로운 Map 생성
            Map<String, Object> attributes = new HashMap<>(originalAttributes);
            
            // 카카오 액세스 토큰을 attributes에 추가
            String accessToken = userRequest.getAccessToken().getTokenValue();
            attributes.put("access_token", accessToken);
            
            // 카카오 사용자 정보 파싱
            String oauthId = String.valueOf(attributes.get("id"));
            Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
            Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
            
            String email = null;
            String name = null;
            
            if (kakaoAccount != null) {
                email = (String) kakaoAccount.get("email");
                if (profile != null) {
                    name = (String) profile.get("nickname");
                }
            }
            
            // 사용자 정보가 없으면 기본값 설정
            if (name == null || name.isEmpty()) {
                name = "사용자";
            }
            if (email == null || email.isEmpty()) {
                email = oauthId + "@kakao.com";
            }
            
            // DB에서 사용자 조회 또는 생성
            User user = userRepository.findByOauthId(oauthId);
            
            if (user == null) {
                // 새 사용자 생성
                user = new User(null, name, email, oauthId, null, "[]");
                userRepository.save(user);
            } else {
                // 기존 사용자 정보 업데이트
                user.setName(name);
                user.setEmail(email);
                userRepository.save(user);
            }
            
            // OAuth2User 객체에 사용자 정보 추가
            return new org.springframework.security.oauth2.core.user.DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                "id"
            );
            
        } catch (Exception e) {
            throw new RuntimeException("OAuth2 사용자 정보 로드 실패", e);
        }
    }
}