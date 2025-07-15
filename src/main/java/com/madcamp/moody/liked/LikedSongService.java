package com.madcamp.moody.liked;

import com.madcamp.moody.user.User;
import com.madcamp.moody.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class LikedSongService {

    private final LikedSongRepository likedSongRepository;
    private final UserRepository userRepository;

    @Autowired
    public LikedSongService(LikedSongRepository likedSongRepository, UserRepository userRepository) {
        this.likedSongRepository = likedSongRepository;
        this.userRepository = userRepository;
    }

    private User getUser(OAuth2User oAuth2User) {
        if (oAuth2User == null) {
            throw new IllegalArgumentException("User not authenticated");
        }
        String oauthId = String.valueOf(oAuth2User.getAttributes().get("id"));
        User user = userRepository.findByOauthId(oauthId);
        if (user == null) {
            throw new IllegalArgumentException("User not found for OAuth ID: " + oauthId);
        }
        return user;
    }

    public LikedSongDTO addLikedSong(LikedSongRequestDTO requestDTO, OAuth2User oAuth2User) {
        User user = getUser(oAuth2User);
        if (likedSongRepository.existsByUserAndTrackId(user, requestDTO.getTrackId())) {
            throw new IllegalStateException("Song already liked");
        }
        LikedSong likedSong = new LikedSong();
        likedSong.setUser(user);
        likedSong.setTrackId(requestDTO.getTrackId());
        likedSong.setMusicUrl(requestDTO.getMusicUrl());
        likedSong.setTitle(requestDTO.getTitle());
        likedSong.setArtist(requestDTO.getArtist());
        
        LikedSong savedSong = likedSongRepository.save(likedSong);
        return LikedSongDTO.fromEntity(savedSong);
    }
    
    public void removeLikedSong(LikedSongRequestDTO requestDTO, OAuth2User oAuth2User) {
        User user = getUser(oAuth2User);
        if (!likedSongRepository.existsByUserAndTrackId(user, requestDTO.getTrackId())) {
            throw new IllegalStateException("Song not found in liked list");
        }
        likedSongRepository.deleteByUserAndTrackId(user, requestDTO.getTrackId());
    }

    @Transactional(readOnly = true)
    public List<LikedSongDTO> getLikedSongs(OAuth2User oAuth2User) {
        User user = getUser(oAuth2User);
        return likedSongRepository.findByUser(user)
                .stream()
                .map(LikedSongDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public Set<String> getLikedTrackIds(OAuth2User oAuth2User) {
        User user = getUser(oAuth2User);
        return likedSongRepository.findByUser(user)
                .stream()
                .map(LikedSong::getTrackId)
                .collect(Collectors.toSet());
    }
} 