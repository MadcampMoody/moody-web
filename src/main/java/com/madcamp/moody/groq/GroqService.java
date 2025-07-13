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
    
    @Autowired
    public GroqService(RestTemplate restTemplate, SpotifyService spotifyService, PlaylistService playlistService, MusicService musicService) {
        this.restTemplate = restTemplate;
        this.spotifyService = spotifyService;
        this.playlistService = playlistService;
        this.musicService = musicService;
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
        // 텍스트 길이에 따른 요약 처리
        String analyzedText = text;
        if (text.length() > 1000) {
            analyzedText = text.substring(0, 1000) + "...";
        }
        
        String prompt = "당신은 사용자의 글을 한 편의 영화 장면처럼 여기고, 그 장면에 완벽하게 어울리는 사운드트랙을 만드는 **음악 감독**이자, 섬세한 **감성 큐레이터**입니다. " +
                "모든 음악 장르와 아티스트에 대한 깊은 지식을 바탕으로 사용자의 감정과 상황에 딱 맞는 음악을 추천해주세요.\n\n" +

                "🎯 **추천 철학**:\n" +
                "- 글의 텍스트에서 느껴지는 감정, 분위기, 그리고 **상황적 맥락(계절, 기념일, 이벤트 등)**을 정확히 파악하여 음악적 경험을 제공합니다.\n" +
                "- 감정에 공감하는 음악, 기분을 전환하는 음악, 새로운 에너지를 주는 음악 등 다양한 접근을 시도합니다.\n" +
                "- 예상치 못한 창의적인 장르 조합과 독특한 키워드 선택을 통해 특별한 플레이리스트를 구성합니다.\n\n" +

                "🎵 **장르 선택 시 고려사항**:\n" +
                "- 장르는 반드시 Spotify에서 검색 가능한 **영어**로 선택해야 합니다.\n" +
                "- 메인스트림부터 언더그라운드까지 모든 장르를 활용하며, **상황에 맞는 장르를 우선적으로 고려**합니다. (예: 크리스마스 -> jazz, carol, classical)\n" +
                "- 특정 역할(DJ 등)에 얽매이지 않고, 글의 내용에 가장 적합한 장르를 자유롭게 선택합니다.\n" +
                "- Spotify에서 검색 가능한 모든 장르를 활용합니다.\n\n" +

                "🎼 **키워드 선택 시 고려사항**:\n" +
                "- 음악의 분위기, 느낌, 에너지를 표현하는 영어 단어를 사용합니다.\n" +
                "- 감정적, 기술적, 분위기적 특성을 모두 활용합니다.\n" +
                "- 음악 검색에 도움이 되는 구체적인 키워드를 선택합니다.\n\n" +

                "⚠️ **중요 지침**:\n" +
                "- 매번 완전히 다른 장르 조합을 시도하되, 항상 글의 핵심 감정과 상황에 기반해야 합니다.\n" +
                "- 뻔한 조합보다는 창의적이고 독특한 조합을 선호합니다.\n\n" +

                "JSON 형식으로만 응답하세요:\n" +
                "{\"genres\":[\"genre1\", \"genre2\", \"genre3\"],\"keywords\":[\"keyword1\", \"keyword2\", \"keyword3\"]}\n\n" +

                "분석할 텍스트: \"" + analyzedText + "\"";

        // 재시도 로직 (최대 3번)
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                GroqDTO.SimpleResponse response = generateContent(prompt);
                GroqDTO.SpotifyAnalysisResult result = parseSpotifyAnalysis(response.getResponse());
                
                // 유효한 결과인지 확인
                if (result.getGenres() != null && !result.getGenres().isEmpty() && 
                    result.getKeywords() != null && !result.getKeywords().isEmpty()) {
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
                    "다음 텍스트의 감정을 분석하고, 글의 상황(예: 크리스마스, 비 오는 날 등)을 고려하여 어울리는 음악 장르 3개와 키워드 3개를 추천해주세요. " +
                    "**장르는 반드시 영어로**, 키워드는 음악의 분위기를 표현하는 영어 단어로 선택하세요. " +
                    "반드시 다음 JSON 형식으로만 응답하세요:\n" +
                    "{\"genres\":[\"genre1\", \"genre2\", \"genre3\"],\"keywords\":[\"keyword1\", \"keyword2\", \"keyword3\"]}\n\n" +
                    "텍스트: \"" + text + "\"";
            
            GroqDTO.SimpleResponse response = generateContent(simplePrompt);
            return parseSpotifyAnalysis(response.getResponse());
            
        } catch (Exception e) {
            System.err.println("Fallback 분석도 실패: " + e.getMessage());
            return new GroqDTO.SpotifyAnalysisResult(
                Arrays.asList("alternative", "neo-soul", "synthwave"),
                Arrays.asList("introspective", "ethereal", "rhythmic")
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
                Arrays.asList("healing", "calming", "uplifting")
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
    public GroqDTO.MusicAnalysisResponse analyzeTextAndRecommendMusic(String text) {
        // 1. 텍스트 분석하여 장르와 키워드 추출
        GroqDTO.SpotifyAnalysisResult analysisResult = analyzeTextForSpotifySearch(text);

        // 2. Spotify에서 음악 추천 받기
        SpotifyDTO.MusicRecommendation spotifyResult = spotifyService.recommendMusicViaPlaylistSearch(
                analysisResult.getGenres(),
                analysisResult.getKeywords()
        );

        // 3. 응답 변환
        List<GroqDTO.MusicAnalysisResponse.RecommendedTrack> recommendedTracks =
                spotifyResult.getTracks().stream()
                        .map(track -> new GroqDTO.MusicAnalysisResponse.RecommendedTrack(
                                track.getTitle(),
                                track.getArtist(),
                                track.getSpotifyUrl(),
                                track.getPreviewUrl(),
                                track.getTrackId() // trackId 추가
                        ))
                        .collect(Collectors.toList());

        // 4. DB에 플레이리스트와 음악 저장
        if (!recommendedTracks.isEmpty()) {
            // 4.1. Playlist 생성 및 저장
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            String title = LocalDateTime.now().format(formatter) + "의 플레이리스트";
            long dummyDiaryId = 1L; // 임시 다이어리 ID
            PlaylistDTO newPlaylistInfo = new PlaylistDTO(0L, title, dummyDiaryId, null);
            PlaylistDTO savedPlaylist = playlistService.createPlaylist(newPlaylistInfo);

            // 4.2. Music 목록 생성 및 저장
            List<MusicDTO> musicToSave = new ArrayList<>();
            for (GroqDTO.MusicAnalysisResponse.RecommendedTrack track : recommendedTracks) {
                MusicDTO musicDTO = new MusicDTO(0L, track.getSpotifyUrl(), savedPlaylist.getPlaylistId());
                musicToSave.add(musicDTO);
            }
            musicService.createMusics(musicToSave);
        }

        // 5. 최종 결과 반환
        return new GroqDTO.MusicAnalysisResponse(analysisResult, recommendedTracks);
    }
} 