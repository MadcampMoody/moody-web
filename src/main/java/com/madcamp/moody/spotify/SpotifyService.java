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
import com.madcamp.moody.music.MusicRegion;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.madcamp.moody.user.User;
import com.madcamp.moody.user.UserRepository;

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

    @Autowired
    private UserRepository userRepository;

    /**
     * 현재 로그인된 사용자의 Spotify 액세스 토큰을 반환
     * 토큰이 만료되었으면 refresh token을 사용해서 자동 갱신
     */
    public String getCurrentUserSpotifyAccessToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        User user = null;
        
        // OAuth2User에서 사용자 식별
        if (authentication.getPrincipal() instanceof OAuth2User) {
            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
            
            // 먼저 Spotify 사용자인지 확인 (display_name 속성이 있으면 Spotify)
            String spotifyDisplayName = oauth2User.getAttribute("display_name");
            if (spotifyDisplayName != null) {
                // Spotify 로그인 사용자
                String spotifyId = oauth2User.getAttribute("id");
                user = userRepository.findBySpotifyOauthId(spotifyId);
            } else {
                // 카카오 로그인 사용자 - Spotify 연동 여부 확인
                String kakaoId = String.valueOf(oauth2User.getAttribute("id"));
                user = userRepository.findByOauthId(kakaoId);
            }
        }
        
        if (user == null) {
            return null;
        }
        
        // 액세스 토큰이 없으면 refresh token으로 갱신 시도
        if (user.getSpotifyAccessToken() == null || user.getSpotifyAccessToken().isEmpty()) {
            if (user.getSpotifyRefreshToken() != null && !user.getSpotifyRefreshToken().isEmpty()) {
                System.out.println("액세스 토큰이 없습니다. Refresh token으로 갱신을 시도합니다.");
                return refreshAccessToken(user);
            }
            return null;
        }
        
        // 액세스 토큰이 있으면 유효성 검사 후 필요시 갱신
        if (isAccessTokenExpired(user.getSpotifyAccessToken())) {
            if (user.getSpotifyRefreshToken() != null && !user.getSpotifyRefreshToken().isEmpty()) {
                System.out.println("액세스 토큰이 만료되었습니다. Refresh token으로 갱신을 시도합니다.");
                return refreshAccessToken(user);
            }
        }
        
        return user.getSpotifyAccessToken();
    }
    
    /**
     * 액세스 토큰이 만료되었는지 확인 (JWT 토큰의 exp 클레임 확인)
     */
    private boolean isAccessTokenExpired(String accessToken) {
        try {
            // JWT 토큰을 .으로 분리
            String[] parts = accessToken.split("\\.");
            if (parts.length != 3) {
                // JWT 형식이 아니면 만료된 것으로 간주
                return true;
            }
            
            // 페이로드 부분을 디코딩
            String payload = parts[1];
            // Base64 패딩 추가
            while (payload.length() % 4 != 0) {
                payload += "=";
            }
            String decodedPayload = new String(Base64.getDecoder().decode(payload));
            
            // JSON 파싱하여 exp 확인
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> claims = objectMapper.readValue(decodedPayload, Map.class);
            
            Long exp = (Long) claims.get("exp");
            if (exp == null) {
                return true;
            }
            
            // 현재 시간과 비교 (1분 여유 시간)
            long currentTime = System.currentTimeMillis() / 1000;
            return exp < (currentTime + 60);
            
        } catch (Exception e) {
            System.err.println("토큰 만료 확인 중 오류: " + e.getMessage());
            // 오류 발생시 만료된 것으로 간주
            return true;
        }
    }
    
    /**
     * Refresh token을 사용해서 새로운 액세스 토큰을 발급받고 DB에 저장
     */
    private String refreshAccessToken(User user) {
        try {
            System.out.println("Refresh token으로 액세스 토큰 갱신 시작...");
            
            // Spotify Token API 호출
            String tokenUrl = "https://accounts.spotify.com/api/token";
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/x-www-form-urlencoded");
            headers.set("Authorization", "Basic " + Base64.getEncoder().encodeToString(
                (clientId + ":" + clientSecret).getBytes()));
            
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "refresh_token");
            body.add("refresh_token", user.getSpotifyRefreshToken());
            
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> tokenData = response.getBody();
                
                String newAccessToken = (String) tokenData.get("access_token");
                String newRefreshToken = (String) tokenData.get("refresh_token"); // 새로운 refresh token이 있을 수 있음
                
                if (newAccessToken != null) {
                    // DB에 새로운 토큰들 저장
                    user.setSpotifyAccessToken(newAccessToken);
                    if (newRefreshToken != null) {
                        user.setSpotifyRefreshToken(newRefreshToken);
                    }
                    userRepository.save(user);
                    
                    System.out.println("액세스 토큰 갱신 성공!");
                    return newAccessToken;
                }
            }
            
            System.err.println("액세스 토큰 갱신 실패: " + response.getStatusCode());
            return null;
            
        } catch (Exception e) {
            System.err.println("액세스 토큰 갱신 중 오류: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 사용자가 Spotify에 연동되어 있는지 확인
     */
    public boolean isCurrentUserSpotifyLinked() {
        String accessToken = getCurrentUserSpotifyAccessToken();
        return accessToken != null && !accessToken.isEmpty();
    }

    /**
     * Spotify 재생 시작 (최근 재생한 곡 또는 기본 플레이리스트)
     */
    public boolean startSpotifyPlayback(String deviceId) {
        String accessToken = getCurrentUserSpotifyAccessToken();
        if (accessToken == null) {
            System.err.println("Spotify 액세스 토큰이 없습니다");
            return false;
        }

        try {
            System.out.println("=== Spotify 재생 시작 요청 ===");
            System.out.println("Device ID: " + deviceId);
            
            // 사용 가능한 디바이스 확인
            if (!checkAvailableDevices(accessToken, deviceId)) {
                System.err.println("사용 가능한 디바이스가 없거나 지정된 디바이스를 찾을 수 없습니다.");
                return false;
            }
            
            // 먼저 최근 재생한 곡들 가져오기
            String recentlyPlayedUrl = "https://api.spotify.com/v1/me/player/recently-played?limit=1";
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.set("Content-Type", "application/json");
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            System.out.println("최근 재생 곡 조회 중...");
            
            ResponseEntity<Map> recentResponse = restTemplate.exchange(
                recentlyPlayedUrl, HttpMethod.GET, entity, Map.class);
            
            System.out.println("최근 재생 곡 조회 응답: " + recentResponse.getStatusCode());

            String trackUri = null;
            if (recentResponse.getStatusCode().is2xxSuccessful() && recentResponse.getBody() != null) {
                Map<String, Object> recentData = recentResponse.getBody();
                List<Map<String, Object>> items = (List<Map<String, Object>>) recentData.get("items");
                
                if (items != null && !items.isEmpty()) {
                    Map<String, Object> track = (Map<String, Object>) items.get(0).get("track");
                    trackUri = (String) track.get("uri");
                    System.out.println("최근 재생한 곡 발견: " + trackUri);
                }
            }

            // 재생 시작 요청
            String playUrl = "https://api.spotify.com/v1/me/player/play";
            if (deviceId != null && !deviceId.isEmpty()) {
                playUrl += "?device_id=" + deviceId;
            }
            
            System.out.println("재생 요청 URL: " + playUrl);

            Map<String, Object> playData = new HashMap<>();
            if (trackUri != null) {
                // 최근 재생한 곡이 있으면 그것을 재생
                playData.put("uris", List.of(trackUri));
                System.out.println("재생할 곡 URI: " + trackUri);
            } else {
                System.out.println("최근 재생한 곡이 없어서 빈 요청으로 진행");
            }

            HttpEntity<Map<String, Object>> playEntity = new HttpEntity<>(playData, headers);
            System.out.println("재생 요청 전송 중...");
            
            ResponseEntity<String> playResponse = restTemplate.exchange(
                playUrl, HttpMethod.PUT, playEntity, String.class);

            System.out.println("재생 요청 응답 상태: " + playResponse.getStatusCode());
            System.out.println("재생 요청 응답 본문: " + playResponse.getBody());
            
            boolean success = playResponse.getStatusCode().is2xxSuccessful();
            System.out.println("Spotify 재생 시작 결과: " + success);
            return success;

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.err.println("Spotify API HTTP 오류: " + e.getStatusCode());
            System.err.println("오류 응답 본문: " + e.getResponseBodyAsString());
            System.err.println("요청한 URL이나 권한에 문제가 있을 수 있습니다.");
            return false;
        } catch (Exception e) {
            System.err.println("Spotify 재생 시작 오류: " + e.getMessage());
            System.err.println("오류 타입: " + e.getClass().getSimpleName());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 사용 가능한 디바이스 확인
     */
    private boolean checkAvailableDevices(String accessToken, String targetDeviceId) {
        try {
            String devicesUrl = "https://api.spotify.com/v1/me/player/devices";
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.set("Content-Type", "application/json");
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> devicesResponse = restTemplate.exchange(
                devicesUrl, HttpMethod.GET, entity, Map.class);
            
            if (devicesResponse.getStatusCode().is2xxSuccessful() && devicesResponse.getBody() != null) {
                Map<String, Object> devicesData = devicesResponse.getBody();
                List<Map<String, Object>> devices = (List<Map<String, Object>>) devicesData.get("devices");
                
                System.out.println("사용 가능한 디바이스 수: " + (devices != null ? devices.size() : 0));
                
                if (devices == null || devices.isEmpty()) {
                    System.err.println("활성화된 Spotify 디바이스가 없습니다. Spotify 앱을 열어주세요.");
                    return false;
                }
                
                // 디바이스 목록 출력
                for (Map<String, Object> device : devices) {
                    String id = (String) device.get("id");
                    String name = (String) device.get("name");
                    String type = (String) device.get("type");
                    Boolean isActive = (Boolean) device.get("is_active");
                    System.out.println("디바이스: " + name + " (ID: " + id + ", 타입: " + type + ", 활성: " + isActive + ")");
                }
                
                // 지정된 디바이스 ID가 있으면 확인
                if (targetDeviceId != null && !targetDeviceId.isEmpty()) {
                    boolean deviceFound = devices.stream()
                        .anyMatch(device -> targetDeviceId.equals(device.get("id")));
                    if (!deviceFound) {
                        System.err.println("지정된 디바이스 ID를 찾을 수 없습니다: " + targetDeviceId);
                        return false;
                    }
                }
                
                return true;
            } else {
                System.err.println("디바이스 조회 실패: " + devicesResponse.getStatusCode());
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("디바이스 확인 오류: " + e.getMessage());
            return false;
        }
    }

    /**
     * Spotify 재생 제어 (재생/일시정지)
     */
    public boolean controlSpotifyPlayback(String action, String deviceId) {
        String accessToken = getCurrentUserSpotifyAccessToken();
        if (accessToken == null) {
            return false;
        }

        try {
            String url = "https://api.spotify.com/v1/me/player/" + action;
            if (deviceId != null && !deviceId.isEmpty()) {
                url += "?device_id=" + deviceId;
            }

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.set("Content-Type", "application/json");

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.PUT, entity, String.class);

            boolean success = response.getStatusCode().is2xxSuccessful();
            System.out.println("Spotify " + action + " 결과: " + success);
            return success;

        } catch (Exception e) {
            System.err.println("Spotify " + action + " 오류: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
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
    public SpotifyDTO.MusicRecommendation recommendMusicViaPlaylistSearch(List<String> genres, List<String> keywords, MusicRegion region) {
        try {
            String accessToken = getAccessToken();
            if (accessToken == null) {
                System.err.println("Spotify 액세스 토큰을 획득할 수 없습니다.");
                return new SpotifyDTO.MusicRecommendation(Collections.emptyList());
            }

            List<SpotifyDTO.MusicRecommendation.RecommendedTrack> finalTracks = new ArrayList<>();
            Set<String> usedArtists = new HashSet<>();
            String keywordString = String.join(" ", keywords);

            int domesticTarget = 0;
            int internationalTarget = 0;

            switch (region) {
                case DOMESTIC:
                    domesticTarget = 8;
                    internationalTarget = 2;
                    break;
                case INTERNATIONAL:
                    domesticTarget = 2;
                    internationalTarget = 8;
                    break;
                case BOTH:
                    domesticTarget = 5;
                    internationalTarget = 5;
                    break;
            }

            // 국내 음악 검색
            if (domesticTarget > 0) {
                List<SpotifyDTO.MusicRecommendation.RecommendedTrack> domesticTracks = 
                    searchAndSelectTracksForMarket(accessToken, "KR", genres, keywordString, usedArtists, domesticTarget);
                finalTracks.addAll(domesticTracks);
            }

            // 해외 음악 검색
            if (internationalTarget > 0) {
                List<SpotifyDTO.MusicRecommendation.RecommendedTrack> internationalTracks = 
                    searchAndSelectTracksForMarket(accessToken, "US", genres, keywordString, usedArtists, internationalTarget);
                finalTracks.addAll(internationalTracks);
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

    private List<SpotifyDTO.MusicRecommendation.RecommendedTrack> searchAndSelectTracksForMarket(
        String accessToken, String market, List<String> genres, String keywordString, Set<String> usedArtists, int totalTarget) {

        List<SpotifyDTO.MusicRecommendation.RecommendedTrack> allTracksFromMarket = new ArrayList<>();
        
        // 1. 마켓에 해당하는 모든 장르의 트랙을 수집
        for (String genre : genres) {
            try {
                String searchQuery = genre + " " + keywordString;
                allTracksFromMarket.addAll(searchPlaylistForGenre(accessToken, searchQuery, market));
            } catch (Exception e) {
                System.err.println(genre + " 장르(" + market + ") 검색 중 오류: " + e.getMessage());
            }
        }
        Collections.shuffle(allTracksFromMarket);

        // 2. 인기도에 따라 3개 그룹으로 분류
        List<SpotifyDTO.MusicRecommendation.RecommendedTrack> veryPopular = new ArrayList<>();
        List<SpotifyDTO.MusicRecommendation.RecommendedTrack> moderatelyPopular = new ArrayList<>();
        List<SpotifyDTO.MusicRecommendation.RecommendedTrack> lesserKnown = new ArrayList<>();

        for (SpotifyDTO.MusicRecommendation.RecommendedTrack track : allTracksFromMarket) {
            if (track.getPopularity() >= 75) veryPopular.add(track);
            else if (track.getPopularity() >= 30) moderatelyPopular.add(track);
            else lesserKnown.add(track);
        }

        // 3. 목표 비율(2:5:3)에 따라 그룹별 목표 곡 수 계산
        List<SpotifyDTO.MusicRecommendation.RecommendedTrack> finalMarketTracks = new ArrayList<>();
        int veryPopularTarget = (int) Math.round(totalTarget * 0.2);
        int moderatelyPopularTarget = (int) Math.round(totalTarget * 0.5);
        int lesserKnownTarget = (int) Math.round(totalTarget * 0.3);

        // 반올림으로 인해 합계가 totalTarget과 다를 경우 조정
        int currentTotal = veryPopularTarget + moderatelyPopularTarget + lesserKnownTarget;
        if(currentTotal > totalTarget) moderatelyPopularTarget -= (currentTotal - totalTarget);
        if(currentTotal < totalTarget) moderatelyPopularTarget += (totalTarget - currentTotal);

        // 4. 각 그룹에서 목표만큼 곡 선택 (중복 아티스트 제외)
        fillFromBucket(finalMarketTracks, veryPopular, veryPopularTarget, usedArtists);
        fillFromBucket(finalMarketTracks, moderatelyPopular, moderatelyPopularTarget, usedArtists);
        fillFromBucket(finalMarketTracks, lesserKnown, lesserKnownTarget, usedArtists);

        // 5. 목표 곡 수를 채우지 못했다면, 다른 그룹에서 추가로 선택
        if (finalMarketTracks.size() < totalTarget) {
            List<SpotifyDTO.MusicRecommendation.RecommendedTrack> fallbackPool = new ArrayList<>();
            fallbackPool.addAll(moderatelyPopular);
            fallbackPool.addAll(lesserKnown);
            fallbackPool.addAll(veryPopular);
            fillFromBucket(finalMarketTracks, fallbackPool, totalTarget - finalMarketTracks.size(), usedArtists);
        }

        System.out.println("마켓(" + market + ") 최종 선택된 곡 수: " + finalMarketTracks.size());
        return finalMarketTracks;
    }

    private void fillFromBucket(List<SpotifyDTO.MusicRecommendation.RecommendedTrack> targetList, 
                                List<SpotifyDTO.MusicRecommendation.RecommendedTrack> sourceBucket, 
                                int count, Set<String> usedArtists) {
        int added = 0;
        for (SpotifyDTO.MusicRecommendation.RecommendedTrack track : sourceBucket) {
            if (added >= count) break;

            String primaryArtist = track.getArtist().split(",|ft\\.|feat\\.|&")[0].trim().toLowerCase();
            if (usedArtists.add(primaryArtist)) {
                targetList.add(track);
                added++;
            }
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
            String accessToken, String searchQuery, String market) throws Exception {
        
        // 플레이리스트 검색
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(searchUrl)
            .queryParam("q", searchQuery)
            .queryParam("type", "playlist")
            .queryParam("limit", 5);

        if (market != null && !market.isEmpty()) {
            builder.queryParam("market", market);
        }
        String playlistSearchUrl = builder.build().toUriString();

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
                                track.getId(),
                                track.getPopularity()
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

    private List<SpotifyDTO.MusicRecommendation.RecommendedTrack> searchPlaylistForGenre(
            String accessToken, String searchQuery) throws Exception {
        return searchPlaylistForGenre(accessToken, searchQuery, null);
    }
} 