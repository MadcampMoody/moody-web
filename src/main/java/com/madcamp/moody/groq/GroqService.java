package com.madcamp.moody.groq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.madcamp.moody.spotify.SpotifyService;
import com.madcamp.moody.spotify.SpotifyDTO;
import com.madcamp.moody.playlist.PlaylistService;
import com.madcamp.moody.playlist.PlaylistDTO;
import com.madcamp.moody.music.MusicService;
import com.madcamp.moody.music.MusicDTO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import com.madcamp.moody.user.User;
import com.madcamp.moody.user.UserRepository;
import org.springframework.security.oauth2.core.user.OAuth2User;
import java.util.Collections;
import com.madcamp.moody.music.MusicRegion;
import com.madcamp.moody.mood.Mood;
import com.madcamp.moody.mood.MoodRepository;
import java.time.LocalDate;
import java.util.Optional;


@Service
public class GroqService {
    
    @Value("${groq.api.key}")
    private String apiKey;
    
    @Value("${groq.api.url}")
    private String apiUrl;
    
    private final RestTemplate restTemplate;
    private final SpotifyService spotifyService;
    private final PlaylistService playlistService;
    private final MusicService musicService;
    private final UserRepository userRepository;
    private final MoodRepository moodRepository;
    
    @Autowired
    public GroqService(RestTemplate restTemplate, SpotifyService spotifyService, PlaylistService playlistService, MusicService musicService, UserRepository userRepository, MoodRepository moodRepository) {
        this.restTemplate = restTemplate;
        this.spotifyService = spotifyService;
        this.playlistService = playlistService;
        this.musicService = musicService;
        this.userRepository = userRepository;
        this.moodRepository = moodRepository;
    }
    
    public GroqDTO.SimpleResponse generateContent(String prompt) {
        return generateContent(prompt, "llama3-8b-8192");
    }
    
    public GroqDTO.SimpleResponse generateContent(String prompt, String model) {
        try {
            // Groq API 요청 구조 생성
            GroqDTO.GroqRequest.Message message = new GroqDTO.GroqRequest.Message("user", prompt);
            GroqDTO.GroqRequest request = new GroqDTO.GroqRequest(
                model,
                Arrays.asList(message),
                4000,
                0.7
            );
            
            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + apiKey);
            headers.set("Content-Type", "application/json");
            
            // HTTP 엔티티 생성
            HttpEntity<GroqDTO.GroqRequest> entity = new HttpEntity<>(request, headers);
            
            // API 호출
            ResponseEntity<GroqDTO.GroqResponse> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.POST,
                entity,
                GroqDTO.GroqResponse.class
            );
            
            return extractResponse(response.getBody());
        } catch (Exception e) {
            System.err.println("Groq API 호출 오류: " + e.getMessage());
            return new GroqDTO.SimpleResponse("AI 응답을 생성할 수 없습니다.");
        }
    }
    
    private GroqDTO.SimpleResponse extractResponse(GroqDTO.GroqResponse response) {
        if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
            GroqDTO.GroqResponse.Choice choice = response.getChoices().get(0);
            if (choice != null && choice.getMessage() != null && choice.getMessage().getContent() != null) {
                return new GroqDTO.SimpleResponse(choice.getMessage().getContent());
            }
        }
        return new GroqDTO.SimpleResponse("응답을 파싱할 수 없습니다.");
    }
    

    
    // 텍스트 분석 - Spotify 검색을 위한 장르 및 키워드로 변환
    public GroqDTO.SpotifyAnalysisResult analyzeTextForSpotifySearch(String text) {
        return analyzeTextForSpotifySearch(text, null);
    }

    public GroqDTO.SpotifyAnalysisResult analyzeTextForSpotifySearch(String text, String preferredGenre) {
        // 텍스트 길이에 따른 요약 처리
        String analyzedText = text;
        if (text.length() > 1000) {
            analyzedText = text.substring(0, 1000) + "...";
        }
        
        // 동적 프롬프트 생성
        String genrePrompt;
        if (preferredGenre != null && !preferredGenre.isEmpty()) {
            genrePrompt = String.format(
                "사용자가 특별히 선호하는 장르는 '%s'입니다. 이 장르와 글의 분위기를 모두 고려하여, 이와 잘 어울리는 **추가 장르 2개**를 추천해주세요. " +
                "최종적으로 '%s'를 포함하여 총 3개의 장르가 되어야 합니다.",
                preferredGenre, preferredGenre
            );
        } else {
            genrePrompt = "글의 내용에 가장 적합한 장르 3개를 자유롭게 선택합니다.";
        }

        String prompt = "당신은 사용자의 글을 한 편의 영화 장면처럼 여기고, 그 장면에 완벽하게 어울리는 사운드트랙을 만드는 **음악 감독**이자, 섬세한 **감성 큐레이터**입니다. " +
                "모든 음악 장르와 아티스트에 대한 깊은 지식을 바탕으로 사용자의 감정과 상황에 딱 맞는 음악을 추천해주세요.\n\n" +

                "🎯 **추천 철학**:\n" +
                "- 글의 텍스트에서 느껴지는 감정, 분위기, 그리고 **상황적 맥락(계절, 기념일, 이벤트 등)**을 정확히 파악하여 음악적 경험을 제공합니다.\n" +
                "- **특히, 크리스마스, 연말, 휴가 등 명확한 상황적 맥락이 있다면, 해당 분위기에 어울리는 장르(예: Christmas, Carol, Jazz)를 반드시 최우선으로 고려하고 포함해야 합니다.**\n" +
                "- **특히, 특정 가수의 이름이 맥락에 포함되어 있다면, 해당 가수의 음악이나 장르를 반드시 최우선으로 고려하고 포함해야 합니다.**\n" +
                "- 감정에 공감하는 음악, 기분을 전환하는 음악, 새로운 에너지를 주는 음악 등 다양한 접근을 시도합니다.\n" +
                "- 예상치 못한 창의적인 장르 조합과 독특한 키워드 선택을 통해 특별한 플레이리스트를 구성합니다.\n\n" +

                "🎵 **장르 선택 시 고려사항**:\n" +
                "- 장르는 반드시 Spotify에서 검색 가능한 **영어**로 선택해야 합니다.\n" +
                "- 메인스트림부터 언더그라운드까지 모든 장르를 활용하며, **상황에 맞는 장르를 우선적으로 고려**합니다. (예: 크리스마스 -> jazz, carol, classical)\n" +
                "- " + genrePrompt + "\n" +
                "- Spotify에서 검색 가능한 모든 장르를 활용합니다.\n\n" +

                "🎼 **키워드 선택 시 고려사항**:\n" +
                "- 음악의 분위기, 느낌, 에너지를 표현하는 영어 단어를 사용합니다.\n" +
                "- 감정적, 기술적, 분위기적 특성을 모두 활용합니다.\n" +
                "- 음악 검색에 도움이 되는 구체적인 키워드 5개를 선택합니다.\n\n" +

                "⚠️ **중요 지침**:\n" +
                "- 매번 완전히 다른 장르 조합을 시도하되, 항상 글의 핵심 감정과 상황에 기반해야 합니다.\n" +
                "- 뻔한 조합보다는 창의적이고 독특한 조합을 선호합니다.\n\n" +

                "JSON 형식으로만 응답하세요:\n" +
                "{\"genres\":[\"genre1\", \"genre2\", \"genre3\"],\"keywords\":[\"keyword1\", \"keyword2\", \"keyword3\", \"keyword4\", \"keyword5\"]}\n\n" +

                "분석할 텍스트: \"" + analyzedText + "\"";

        // 재시도 로직 (최대 3번)
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                GroqDTO.SimpleResponse response = generateContent(prompt);
                GroqDTO.SpotifyAnalysisResult result = parseSpotifyAnalysis(response.getResponse());
                
                // 장르 개수 보정 (3개 미만일 경우)
                if (result.getGenres() != null && result.getGenres().size() < 3) {
                    System.out.println("AI가 " + result.getGenres().size() + "개의 장르만 반환하여, 기본 장르로 보충합니다.");
                    List<String> defaultGenres = Arrays.asList("pop", "acoustic", "electronic", "rock", "hip-hop");
                    for (String defaultGenre : defaultGenres) {
                        if (result.getGenres().size() >= 3) break;
                        if (!result.getGenres().contains(defaultGenre)) {
                            result.getGenres().add(defaultGenre);
                        }
                    }
                }

                // 사용자가 선호하는 장르를 결과에 포함 (AI가 빠뜨렸을 경우 대비)
                if (preferredGenre != null && !preferredGenre.isEmpty() && result.getGenres() != null && !result.getGenres().contains(preferredGenre)) {
                    result.getGenres().add(0, preferredGenre); // 가장 앞에 추가
                    // 총 3개를 유지하기 위해 마지막 요소 제거
                    if (result.getGenres().size() > 3) {
                        result.getGenres().remove(result.getGenres().size() - 1);
                    }
                }

                // 키워드 개수 5개로 맞추기 (초과 시)
                if (result.getKeywords() != null && result.getKeywords().size() > 5) {
                    result.setKeywords(new ArrayList<>(result.getKeywords().subList(0, 5)));
                }

                // 유효한 결과인지 확인
                if (result.getGenres() != null && result.getGenres().size() == 3 && 
                    result.getKeywords() != null && result.getKeywords().size() == 5) {
                    return result;
                }
                
                System.out.println("시도 " + attempt + ": 유효하지 않은 결과, 재시도 중...");
                
            } catch (Exception e) {
                System.err.println("시도 " + attempt + " 실패: " + e.getMessage());
                if (attempt == 3) {
                    // 마지막 시도에서도 실패하면 간단한 프롬프트로 재시도
                    return fallbackAnalysis(analyzedText);
                }
            }
        }
        
        return fallbackAnalysis(analyzedText);
    }
    
    // 간단한 프롬프트로 재시도하는 fallback 메서드
    private GroqDTO.SpotifyAnalysisResult fallbackAnalysis(String text) {
        try {
            String simplePrompt = "당신은 글의 감성과 상황에 맞는 음악을 추천하는 음악 큐레이터입니다. " +
                    "다음 텍스트의 감정을 분석하고, 글의 상황(예: 크리스마스, 비 오는 날 등)을 고려하여 어울리는 음악 장르 3개와 키워드 5개를 추천해주세요. " +
                    "**장르는 반드시 영어로**, 키워드는 음악의 분위기를 표현하는 영어 단어로 선택하세요. " +
                    "반드시 다음 JSON 형식으로만 응답하세요:\n" +
                    "{\"genres\":[\"genre1\", \"genre2\", \"genre3\"],\"keywords\":[\"keyword1\", \"keyword2\", \"keyword3\", \"keyword4\", \"keyword5\"]}\n\n" +
                    "텍스트: \"" + text + "\"";
            
            GroqDTO.SimpleResponse response = generateContent(simplePrompt);
            GroqDTO.SpotifyAnalysisResult result = parseSpotifyAnalysis(response.getResponse());

            // 장르 개수 보정 (3개 미만일 경우)
            if (result.getGenres() != null && result.getGenres().size() < 3) {
                System.out.println("Fallback 분석에서 AI가 " + result.getGenres().size() + "개의 장르만 반환하여, 기본 장르로 보충합니다.");
                List<String> defaultGenres = Arrays.asList("alternative", "neo-soul", "synthwave", "pop", "r&b");
                for (String defaultGenre : defaultGenres) {
                    if (result.getGenres().size() >= 3) break;
                    if (!result.getGenres().contains(defaultGenre)) {
                        result.getGenres().add(defaultGenre);
                    }
                }
            }

            // 키워드 개수 5개로 맞추기 (초과 시)
            if (result.getKeywords() != null && result.getKeywords().size() > 5) {
                result.setKeywords(new ArrayList<>(result.getKeywords().subList(0, 5)));
            }

            return result;
            
        } catch (Exception e) {
            System.err.println("Fallback 분석도 실패: " + e.getMessage());
            return new GroqDTO.SpotifyAnalysisResult(
                Arrays.asList("alternative", "neo-soul", "synthwave"),
                Arrays.asList("introspective", "ethereal", "rhythmic", "groovy", "dreamy")
            );
        }
    }

    private GroqDTO.SpotifyAnalysisResult parseSpotifyAnalysis(String jsonResponse) {
        System.out.println("AI로부터 받은 원본 응답: " + jsonResponse); // 디버깅용 로그 추가
        
        // 응답 텍스트를 더 철저하게 정리
        String cleanJson = jsonResponse.trim();
        if (cleanJson.startsWith("```json")) {
            cleanJson = cleanJson.substring(7);
        }
        if (cleanJson.endsWith("```")) {
            cleanJson = cleanJson.substring(0, cleanJson.length() - 3);
        }
        cleanJson = cleanJson.trim();

        // 1차 시도: 정상적인 JSON 파싱
        try {
            // JSON 시작과 끝 찾기
            int startIndex = cleanJson.indexOf("{");
            int endIndex = cleanJson.lastIndexOf("}");
            
            if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
                cleanJson = cleanJson.substring(startIndex, endIndex + 1);
            }
            
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(cleanJson, GroqDTO.SpotifyAnalysisResult.class);
            
        } catch (Exception e) {
            System.err.println("1차 JSON 파싱 실패: " + e.getMessage());
            
            // 2차 시도: 정규식으로 필드 추출
            try {
                List<String> genres = extractArrayFromJson(jsonResponse, "genres");
                List<String> keywords = extractArrayFromJson(jsonResponse, "keywords");
                
                if (!genres.isEmpty() && !keywords.isEmpty()) {
                    return new GroqDTO.SpotifyAnalysisResult(genres, keywords);
                }
            } catch (Exception e2) {
                System.err.println("2차 정규식 파싱 실패: " + e2.getMessage());
            }
            
            // 3차 시도: 기본값 반환
            System.err.println("모든 파싱 시도 실패, 기본값 반환");
            return new GroqDTO.SpotifyAnalysisResult(
                Arrays.asList("pop", "acoustic", "ambient"), 
                Arrays.asList("healing", "calming", "uplifting", "soothing", "peaceful")
            );
        }
    }
    
    private List<String> extractArrayFromJson(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*\\[([^\\]]+)\\]";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        
        if (m.find()) {
            String arrayContent = m.group(1);
            return Arrays.stream(arrayContent.split(","))
                .map(s -> s.trim().replaceAll("^\"|\"$", ""))
                .collect(Collectors.toList());
        }
        return Arrays.asList();
    }
    
    private String extractStringFromJson(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]+)\"";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.DOTALL);
        java.util.regex.Matcher m = p.matcher(json);
        
        if (m.find()) {
            return m.group(1).replace("\\n", "\n").replace("\\\"", "\"");
        }
        return "";
    }

    private String extractStringValue(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*\"(.*?)\"";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        return ""; // 기본값
    }
    
    // 텍스트 분석 + 음악 추천 (Playlist Search API 사용)
    public GroqDTO.MusicAnalysisResponse analyzeTextAndRecommendMusic(String text, String date, OAuth2User oAuth2User) {
        // OAuth2User에서 User 엔티티 조회
        String oauthId = String.valueOf(oAuth2User.getAttributes().get("id"));
        User user = userRepository.findByOauthId(oauthId);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
        }

        String analysisText = text;
        if (analysisText == null || analysisText.trim().isEmpty()) {
            if (date != null && !date.trim().isEmpty()) {
                LocalDate localDate = LocalDate.parse(date);
                Optional<Mood> moodOptional = Optional.ofNullable(moodRepository.findByUserAndDate(user, localDate));
                if (moodOptional.isPresent()) {
                    // MoodType enum을 설명적인 텍스트로 변환 (예: "Happy")
                    analysisText = "Today's mood is " + moodOptional.get().getMoodType().name().toLowerCase() + ".";
                } else {
                    analysisText = "a normal day"; // 해당 날짜에 기분이 없으면 기본값
                }
            } else {
                 analysisText = "a normal day"; // 날짜 정보도 없으면 기본값
            }
        }

        // 0. 사용자 선호 장르 선택
        List<String> userGenres = user.getMusicGenresList();
        String selectedUserGenre = null;
        if (userGenres != null && !userGenres.isEmpty()) {
            selectedUserGenre = userGenres.get(new Random().nextInt(userGenres.size()));
        }

        // 1. 텍스트 분석하여 장르와 키워드 추출
        GroqDTO.SpotifyAnalysisResult analysisResult = analyzeTextForSpotifySearch(analysisText, selectedUserGenre);

        // 2. Spotify에서 음악 추천 받기
        MusicRegion region = user.getMusicRegion() != null ? user.getMusicRegion() : MusicRegion.BOTH;
        SpotifyDTO.MusicRecommendation spotifyResult = spotifyService.recommendMusicViaPlaylistSearch(
                analysisResult.getGenres(),
                analysisResult.getKeywords(),
                region
        );

        // 사용자가 이전에 추천받은 모든 트랙 ID 조회
        Set<String> previouslyRecommendedTrackIds = new HashSet<>(musicService.findTrackIdsByUserId(user.getId()));

        // 3. 응답 변환 및 아티스트 중복 제거 (10곡 보장 로직)
        Set<String> processedArtists = new HashSet<>();
        List<GroqDTO.MusicAnalysisResponse.RecommendedTrack> recommendedTracks = new ArrayList<>();
        List<SpotifyDTO.MusicRecommendation.RecommendedTrack> spotifyTracks = new ArrayList<>(spotifyResult.getTracks());

        int attempts = 0;
        final int MAX_ATTEMPTS = 5; // 무한 루프 방지를 위한 최대 시도 횟수

        while (recommendedTracks.size() < 10 && attempts < MAX_ATTEMPTS) {
            // 현재 가지고 있는 트랙 목록에서 중복되지 않는 아티스트의 곡을 추가
            List<SpotifyDTO.MusicRecommendation.RecommendedTrack> currentTracks = new ArrayList<>(spotifyTracks);
            Collections.shuffle(currentTracks); // 트랙 순서를 섞어 매번 다른 곡이 선택될 확률을 높임

            for (SpotifyDTO.MusicRecommendation.RecommendedTrack track : currentTracks) {
                if (recommendedTracks.size() >= 10) {
                    break;
                }

                // 이전에 추천된 곡인지 확인
                if (previouslyRecommendedTrackIds.contains(track.getTrackId())) {
                    continue;
                }

                String artistName = track.getArtist();
                if (artistName == null || artistName.isEmpty()) {
                    continue;
                }

                // instrumental 트랙 건너뛰기
                if (track.getTitle() != null && track.getTitle().toLowerCase().contains("instrumental")) {
                    continue;
                }
                
                String primaryArtist = artistName.split(",|ft\\.|feat\\.|&")[0].trim().toLowerCase();
                if (processedArtists.add(primaryArtist)) {
                    recommendedTracks.add(new GroqDTO.MusicAnalysisResponse.RecommendedTrack(
                            track.getTitle(),
                            track.getArtist(),
                            track.getSpotifyUrl(),
                            track.getPreviewUrl(),
                            track.getTrackId()
                    ));
                }
            }

            // 10곡을 채우지 못했다면, 추가로 음악 검색
            if (recommendedTracks.size() < 10) {
                attempts++;
                if (attempts >= MAX_ATTEMPTS) {
                    System.out.println("최대 시도(" + MAX_ATTEMPTS + "회)에 도달하여 추가 검색을 중단합니다.");
                    break;
                }

                System.out.println((attempts) + "차 시도: 플레이리스트에 " + recommendedTracks.size() + "곡, 10곡을 채우기 위해 추가 검색.");

                // 다음 검색을 위한 새로운 쿼리 생성
                SpotifyDTO.MusicRecommendation moreSpotifyResult = spotifyService.recommendMusicViaPlaylistSearch(
                        analysisResult.getGenres(), // 동일 장르 목록을 다시 사용
                        analysisResult.getKeywords(), // 동일 키워드 목록을 다시 사용
                        region
                );
                spotifyTracks = moreSpotifyResult.getTracks();
            }
        }

        if (recommendedTracks.size() < 10) {
            System.out.println("최종적으로 10곡을 채우지 못했습니다. 현재 곡 수: " + recommendedTracks.size());
        }

        // 4. DB에 플레이리스트와 음악 저장
        // 날짜 파싱 - 안전장치 추가
        LocalDate playlistDate;
        try {
            if (date != null && !date.trim().isEmpty()) {
                playlistDate = LocalDate.parse(date);
            } else {
                playlistDate = LocalDate.now(); // 날짜가 없으면 현재 날짜 사용
            }
        } catch (Exception e) {
            System.err.println("날짜 파싱 오류: " + e.getMessage() + ", 현재 날짜를 사용합니다.");
            playlistDate = LocalDate.now();
        }
        
        String title = playlistDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + "의 플레이리스트";
        
        // playlist 생성 또는 업데이트 (같은 날짜에 기존 플레이리스트가 있으면 덮어씀)
        PlaylistDTO newPlaylistInfo = new PlaylistDTO(title, user.getId(), playlistDate);
        PlaylistDTO savedPlaylist = playlistService.createOrUpdatePlaylist(newPlaylistInfo);

        // 4.2. Music 목록 생성 및 저장
        List<MusicDTO> musicToSave = new ArrayList<>();
        for (GroqDTO.MusicAnalysisResponse.RecommendedTrack track : recommendedTracks) {
            MusicDTO musicDTO = new MusicDTO(0L, track.getSpotifyUrl(), savedPlaylist.getPlaylistId());
            musicDTO.setUserId(user.getId()); // 사용자 ID 설정
            musicToSave.add(musicDTO);
        }
        musicService.createMusics(musicToSave);

        // 5. 최종 결과 반환
        return new GroqDTO.MusicAnalysisResponse(analysisResult, recommendedTracks, savedPlaylist.getTitle());
    }
} 