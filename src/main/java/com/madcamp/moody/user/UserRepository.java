package com.madcamp.moody.user;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByOauthId(String oauthId);
    User findBySpotifyOauthId(String spotifyOauthId);
}