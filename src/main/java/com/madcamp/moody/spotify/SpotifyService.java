package com.madcamp.moody.spotify;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.Collections;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

@Service
public class SpotifyService {
    
    @Value("${spotify.client.id}")
    private String clientId;
    
    @Value("${spotify.client.secret}")
    private String clientSecret;
    
    @Value("${spotify.token.url}")
    private String tokenUrl;
    
    @Value("${spotify.search.url}")
    private String searchUrl;
    
    @Value("${spotify.recommend.url}")
    private String recommendUrl;
    
    @Value("${spotify.playlist.tracks.url.template}")
    private String playlistTracksUrlTemplate;
    
    private final RestTemplate restTemplate;
    private String accessToken;
    private long tokenExpiryTime;
    
    @Autowired
    public SpotifyService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    private String getAccessToken() {
        // 토큰이 유효하면 기존 토큰 반환
        if (accessToken != null && System.currentTimeMillis() < tokenExpiryTime) {
            System.out.println("기존 토큰 사용 중...");
            return accessToken;
        }
        
        System.out.println("새로운 Spotify 토큰 요청 중...");
        System.out.println("Client ID: " + clientId);
        System.out.println("Token URL: " + tokenUrl);
        
        try {
            // Client Credentials Flow
            HttpHeaders headers = new HttpHeaders();
            String credentials = clientId + ":" + clientSecret;
            String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());
            headers.set("Authorization", "Basic " + encodedCredentials);
            headers.set("Content-Type", "application/x-www-form-urlencoded");
            
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "client_credentials");
            
            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);
            
            System.out.println("토큰 요청 전송 중...");
            ResponseEntity<SpotifyDTO.TokenResponse> response = restTemplate.exchange(
                tokenUrl,
                HttpMethod.POST,
                entity,
                SpotifyDTO.TokenResponse.class
            );
            
            System.out.println("토큰 응답 상태: " + response.getStatusCode());
            
            if (response.getBody() != null) {
                accessToken = response.getBody().getAccessToken();
                // 만료 시간을 현재 시간 + expires_in - 60초 (여유분)로 설정
                tokenExpiryTime = System.currentTimeMillis() + (response.getBody().getExpiresIn() - 60) * 1000L;
                System.out.println("토큰 획득 성공!");
                return accessToken;
            } else {
                System.err.println("토큰 응답 body가 null입니다.");
            }
        } catch (Exception e) {
            System.err.println("Spotify 토큰 획득 오류: " + e.getMessage());
            e.printStackTrace();
            System.err.println("Client ID: " + clientId);
            System.err.println("Client Secret: " + (clientSecret != null ? clientSecret.substring(0, 4) + "****" : "null"));
            System.err.println("Token URL: " + tokenUrl);
        }
        
        return null;
    }

    // 장르와 키워드로 플레이리스트 검색 후, 해당 플레이리스트의 트랙들을 가져오는 로직
    public SpotifyDTO.MusicRecommendation recommendMusicViaPlaylistSearch(List<String> genres, List<String> keywords) {
        try {
            String accessToken = getAccessToken();
            if (accessToken == null) {
                System.err.println("Spotify 액세스 토큰을 획득할 수 없습니다.");
                return new SpotifyDTO.MusicRecommendation(Collections.emptyList());
            }

            List<SpotifyDTO.MusicRecommendation.RecommendedTrack> finalTracks = new ArrayList<>();
            Set<String> usedArtists = new HashSet<>(); // 중복 아티스트 방지
            String keywordString = String.join(" ", keywords);
            
            // 각 장르별 목표 곡 수 설정
            int[] targetCounts = {4, 3, 3}; // 첫번째 장르 4곡, 두번째/세번째 장르 각 3곡

            // 각 장르별로 개별 검색하여 다양한 음악 추천
            for (int i = 0; i < genres.size() && i < targetCounts.length; i++) {
                String genre = genres.get(i);
                int targetCount = targetCounts[i];
                
                try {
                    // 장르 + 키워드 조합으로 검색
                    String searchQuery = genre + " " + keywordString;
                    System.out.println("장르별 검색 쿼리: " + searchQuery + " (목표: " + targetCount + "곡)");
                    
                    List<SpotifyDTO.MusicRecommendation.RecommendedTrack> genreTracks = 
                        searchPlaylistForGenre(accessToken, searchQuery);
                    
                    // 중복 아티스트 제거하면서 목표 곡 수만큼 선택
                    List<SpotifyDTO.MusicRecommendation.RecommendedTrack> selectedTracks = 
                        selectTracksWithoutDuplicateArtists(genreTracks, usedArtists, targetCount);
                    
                    finalTracks.addAll(selectedTracks);
                    System.out.println(genre + " 장르에서 " + selectedTracks.size() + "곡 추가 (중복 아티스트 제거 후)");
                    
                } catch (Exception e) {
                    System.err.println(genre + " 장르 검색 중 오류: " + e.getMessage());
                }
            }

            System.out.println("최종 추천된 트랙 수: " + finalTracks.size());
            System.out.println("사용된 아티스트 수: " + usedArtists.size());
            return new SpotifyDTO.MusicRecommendation(finalTracks);

        } catch (Exception e) {
            System.err.println("Spotify 플레이리스트 기반 추천 오류: " + e.getMessage());
            e.printStackTrace();
            return new SpotifyDTO.MusicRecommendation(Collections.emptyList());
        }
    }

    // 중복 아티스트 없이 트랙 선택
    private List<SpotifyDTO.MusicRecommendation.RecommendedTrack> selectTracksWithoutDuplicateArtists(
            List<SpotifyDTO.MusicRecommendation.RecommendedTrack> tracks, 
            Set<String> usedArtists, 
            int targetCount) {
        
        List<SpotifyDTO.MusicRecommendation.RecommendedTrack> selectedTracks = new ArrayList<>();
        
        // 트랙을 섞어서 다양성 증대
        Collections.shuffle(tracks);
        
        for (SpotifyDTO.MusicRecommendation.RecommendedTrack track : tracks) {
            if (selectedTracks.size() >= targetCount) {
                break; // 목표 곡 수 달성
            }
            
            // 아티스트 이름을 정규화 (소문자, 공백 제거)
            String normalizedArtist = track.getArtist().toLowerCase().replaceAll("\\s+", "");
            
            // 중복 아티스트 체크
            if (!usedArtists.contains(normalizedArtist)) {
                selectedTracks.add(track);
                usedArtists.add(normalizedArtist);
                System.out.println("선택된 트랙: " + track.getTitle() + " - " + track.getArtist());
            } else {
                System.out.println("중복 아티스트로 제외: " + track.getTitle() + " - " + track.getArtist());
            }
        }
        
        return selectedTracks;
    }

    // 특정 장르와 키워드로 플레이리스트 검색하여 트랙 반환
    private List<SpotifyDTO.MusicRecommendation.RecommendedTrack> searchPlaylistForGenre(
            String accessToken, String searchQuery) throws Exception {
        
        // 플레이리스트 검색
        String playlistSearchUrl = UriComponentsBuilder.fromHttpUrl(searchUrl)
            .queryParam("q", searchQuery)
            .queryParam("type", "playlist")
            .queryParam("limit", 5) // 더 많은 플레이리스트 검색으로 다양성 증대
            .build().toUriString();

        HttpHeaders searchHeaders = new HttpHeaders();
        searchHeaders.setBearerAuth(accessToken);
        HttpEntity<String> searchEntity = new HttpEntity<>(searchHeaders);

        ResponseEntity<SpotifyDTO.PlaylistSearchResponse> searchResponse = restTemplate.exchange(
            playlistSearchUrl, HttpMethod.GET, searchEntity, SpotifyDTO.PlaylistSearchResponse.class);

        if (searchResponse.getBody() == null || 
            searchResponse.getBody().getPlaylists() == null ||
            searchResponse.getBody().getPlaylists().getItems().isEmpty()) {
            return Collections.emptyList();
        }

        List<SpotifyDTO.MusicRecommendation.RecommendedTrack> tracks = new ArrayList<>();

        // 각 플레이리스트에서 몇 곡씩 가져오기
        for (SpotifyDTO.SimplifiedPlaylist playlist : searchResponse.getBody().getPlaylists().getItems()) {
            if (playlist == null || playlist.getId() == null) continue;

            try {
                String tracksUrl = playlistTracksUrlTemplate.replace("{playlist_id}", playlist.getId());
                tracksUrl = UriComponentsBuilder.fromHttpUrl(tracksUrl)
                    .queryParam("limit", 8) // 각 플레이리스트당 더 많은 곡 수집
                    .build().toUriString();

                ResponseEntity<SpotifyDTO.PlaylistTracksResponse> tracksResponse = restTemplate.exchange(
                    tracksUrl, HttpMethod.GET, searchEntity, SpotifyDTO.PlaylistTracksResponse.class);

                if (tracksResponse.getBody() != null && tracksResponse.getBody().getItems() != null) {
                    List<SpotifyDTO.MusicRecommendation.RecommendedTrack> playlistTracks = 
                        tracksResponse.getBody().getItems().stream()
                            .filter(item -> item != null && item.getTrack() != null)
                            .map(item -> item.getTrack())
                            .filter(track -> track != null && track.getName() != null && 
                                   track.getArtists() != null && !track.getArtists().isEmpty())
                            .map(track -> new SpotifyDTO.MusicRecommendation.RecommendedTrack(
                                track.getName(),
                                track.getArtists().stream()
                                    .filter(artist -> artist != null && artist.getName() != null)
                                    .map(SpotifyDTO.Artist::getName)
                                    .collect(Collectors.joining(", ")),
                                track.getExternalUrls() != null ? track.getExternalUrls().getSpotify() : "",
                                track.getPreviewUrl(),
                                track.getId()
                            ))
                            .collect(Collectors.toList());

                    tracks.addAll(playlistTracks);
                }
            } catch (Exception e) {
                System.err.println("플레이리스트 트랙 가져오기 실패: " + e.getMessage());
            }
        }

        return tracks;
    }
} 