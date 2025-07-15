package com.madcamp.moody.mood;

import com.madcamp.moody.user.User;
import com.madcamp.moody.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import com.madcamp.moody.diary.DiaryRepository;
import com.madcamp.moody.diary.Diary;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/mood")
@CrossOrigin(origins = "http://127.0.0.1:3000", allowCredentials = "true")
public class MoodController {

    @Autowired
    private MoodRepository moodRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DiaryRepository diaryRepository;

    @PostMapping
    public ResponseEntity<?> saveMood(
            @RequestBody MoodRequestDTO request,
            @AuthenticationPrincipal OAuth2User oauth2User
    ) {
        try {
            System.out.println("=== 감정 저장 요청 시작 ===");
            System.out.println("받은 요청 데이터: " + request.getDate() + ", " + request.getMoodType());
            
            if (oauth2User == null) {
                System.err.println("OAuth2User가 null입니다.");
                return ResponseEntity.status(401).body("인증이 필요합니다.");
            }

            User user = null;
            
            // Spotify 사용자인지 확인 (display_name 속성이 있으면 Spotify)
            String spotifyDisplayName = oauth2User.getAttribute("display_name");
            if (spotifyDisplayName != null) {
                // Spotify 로그인 사용자
                String spotifyId = oauth2User.getAttribute("id");
                user = userRepository.findBySpotifyOauthId(spotifyId);
                System.out.println("Spotify 사용자: " + spotifyId);
            } else {
                // 카카오 로그인 사용자
                String kakaoId = oauth2User.getAttribute("id").toString();
                user = userRepository.findByOauthId(kakaoId);
                System.out.println("카카오 사용자: " + kakaoId);
            }
            
            if (user == null) {
                System.err.println("사용자를 찾을 수 없습니다.");
                return ResponseEntity.status(404).body("사용자를 찾을 수 없습니다");
            }

            System.out.println("사용자 찾음: " + user.getId());

            // 2. 감정 enum 변환
            MoodType moodType;
            try {
                moodType = MoodType.valueOf(request.getMoodType());
                System.out.println("감정 타입 변환 성공: " + moodType);
            } catch (IllegalArgumentException e) {
                System.err.println("잘못된 감정 타입: " + request.getMoodType());
                return ResponseEntity.status(400).body("잘못된 감정 타입입니다: " + request.getMoodType());
            }

            // 3. 날짜 파싱
            LocalDate date;
            try {
                date = LocalDate.parse(request.getDate());
                System.out.println("날짜 파싱 성공: " + date);
            } catch (Exception e) {
                System.err.println("날짜 파싱 실패: " + request.getDate());
                return ResponseEntity.status(400).body("잘못된 날짜 형식입니다: " + request.getDate());
            }

            // 4. 기존 감정이 있는지 확인
            Mood existingMood = moodRepository.findByUserAndDate(user, date);
            
            Mood savedMood;
            if (existingMood != null) {
                // 기존 감정 업데이트
                System.out.println("기존 감정 업데이트: " + existingMood.getId());
                existingMood.setMoodType(moodType);
                savedMood = moodRepository.save(existingMood);
            } else {
                // 새 감정 생성
                System.out.println("새 감정 생성");
                Mood mood = new Mood();
                mood.setUser(user);
                mood.setMoodType(moodType);
                mood.setDate(date);
                savedMood = moodRepository.save(mood);
            }

            System.out.println("감정 저장 성공: " + savedMood.getId());

            // 프론트엔드에서 사용할 수 있도록 저장된 Mood 객체를 DTO로 변환하여 반환
            return ResponseEntity.ok(new MoodDTO(savedMood));
            
        } catch (Exception e) {
            System.err.println("=== 감정 저장 중 오류 발생 ===");
            System.err.println("Error in saveMood: " + e.getMessage());
            System.err.println("Error class: " + e.getClass().getSimpleName());
            e.printStackTrace();
            
            // 더 구체적인 에러 메시지 반환
            String errorMessage = "서버 오류가 발생했습니다";
            if (e.getMessage() != null) {
                errorMessage += ": " + e.getMessage();
            }
            
            return ResponseEntity.status(500).body(errorMessage);
        }
    }

    @GetMapping("/{date}")
    public ResponseEntity<?> getMoodAndDiary(
        @PathVariable String date,
        @AuthenticationPrincipal OAuth2User oauth2User
    ) {
        try {
            if (oauth2User == null) {
                return ResponseEntity.status(401).body("인증이 필요합니다.");
            }

            User user = null;
            
            // Spotify 사용자인지 확인 (display_name 속성이 있으면 Spotify)
            String spotifyDisplayName = oauth2User.getAttribute("display_name");
            if (spotifyDisplayName != null) {
                // Spotify 로그인 사용자
                String spotifyId = oauth2User.getAttribute("id");
                user = userRepository.findBySpotifyOauthId(spotifyId);
            } else {
                // 카카오 로그인 사용자
                String kakaoId = oauth2User.getAttribute("id").toString();
                user = userRepository.findByOauthId(kakaoId);
            }

            if (user == null) {
                return ResponseEntity.status(404).body("사용자를 찾을 수 없습니다.");
            }

            // 감정 조회
            Mood mood = moodRepository.findByUserAndDate(user, LocalDate.parse(date));
            // 일기 조회 (Diary 테이블이 있다면)
            Diary diary = null;

            if (mood != null) {
                diary = diaryRepository.findByMood(mood);
            }

            // 임시로 일기 내용은 빈 문자열로 반환
            Map<String, Object> result = new HashMap<>();
            result.put("mood", mood != null ? new MoodDTO(mood) : null);
            result.put("diary", diary != null ? diary : null);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.err.println("무드/다이어리 조회 오류: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("서버 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @GetMapping("/month")
    public ResponseEntity<?> getMonthlyMoods(
        @RequestParam int year,
        @RequestParam int month,
        @AuthenticationPrincipal OAuth2User oauth2User
    ) {
        try {
            if (oauth2User == null) {
                return ResponseEntity.status(401).body("인증이 필요합니다.");
            }

            User user = null;
            
            // Spotify 사용자인지 확인 (display_name 속성이 있으면 Spotify)
            String spotifyDisplayName = oauth2User.getAttribute("display_name");
            if (spotifyDisplayName != null) {
                // Spotify 로그인 사용자
                String spotifyId = oauth2User.getAttribute("id");
                user = userRepository.findBySpotifyOauthId(spotifyId);
            } else {
                // 카카오 로그인 사용자
                String kakaoId = oauth2User.getAttribute("id").toString();
                user = userRepository.findByOauthId(kakaoId);
            }

            if (user == null) {
                return ResponseEntity.status(404).body("사용자를 찾을 수 없습니다.");
            }

            List<MoodDTO> moodDTOs = new ArrayList<>();
            LocalDate startDate = LocalDate.of(year, month, 1);
            LocalDate endDate = LocalDate.of(year, month, LocalDate.of(year, month, 1).lengthOfMonth());

            List<Mood> moods = moodRepository.findByUserAndDateBetween(user, startDate, endDate);

            for (Mood mood : moods) {
                moodDTOs.add(new MoodDTO(mood));
            }

            return ResponseEntity.ok(moodDTOs);
        } catch (Exception e) {
            System.err.println("월별 무드 조회 오류: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("서버 오류가 발생했습니다: " + e.getMessage());
        }
    }
}
