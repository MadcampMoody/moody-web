package com.madcamp.moody.user;

import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class OAuth2UserServiceRouter implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    @Autowired
    private SpotifyOAuth2UserService spotifyOAuth2UserService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        System.out.println("OAuth2 로그인 시도 - Provider: " + registrationId);
        if ("spotify".equals(registrationId)) {
            System.out.println("Spotify OAuth2UserService로 라우팅");
            return spotifyOAuth2UserService.loadUser(userRequest);
        } else {
            throw new IllegalArgumentException("지원하지 않는 OAuth2 제공자: " + registrationId);
        }
    }
} 