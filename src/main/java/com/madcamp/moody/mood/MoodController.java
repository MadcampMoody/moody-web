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
            // 1. 로그인한 사용자 찾기
            String oauthId = oauth2User.getAttribute("id").toString();
            User user = userRepository.findByOauthId(oauthId);
            
            if (user == null) {
                return ResponseEntity.status(404).body("사용자를 찾을 수 없습니다");
            }

            // 2. 감정 enum 변환
            MoodType moodType = MoodType.valueOf(request.getMoodType());

            // 3. Mood 엔티티 생성 및 저장
            Mood mood = new Mood();
            mood.setUser(user);
            mood.setMoodType(moodType);
            mood.setDate(LocalDate.parse(request.getDate()));

            moodRepository.save(mood);

            return ResponseEntity.ok().body("감정 기록 완료!");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body("잘못된 감정 타입입니다: " + request.getMoodType());
        } catch (Exception e) {
            System.err.println("Error in saveMood: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("서버 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @GetMapping("/{date}")
    public ResponseEntity<?> getMoodAndDiary(
        @PathVariable String date,
        @AuthenticationPrincipal OAuth2User oauth2User
    ) {
        String oauthId = oauth2User.getAttribute("id").toString();
        User user = userRepository.findByOauthId(oauthId);

        if (user == null) {
            return ResponseEntity.status(404).body("User not found");
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
    }

    @GetMapping("/month")
    public ResponseEntity<?> getMonthlyMoods(
        @RequestParam int year,
        @RequestParam int month,
        @AuthenticationPrincipal OAuth2User oauth2User
    ) {
        String oauthId = oauth2User.getAttribute("id").toString();
        User user = userRepository.findByOauthId(oauthId);

        if (user == null) {
            return ResponseEntity.status(404).body("User not found");
        }

        List<MoodDTO> moodDTOs = new ArrayList<>();
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = LocalDate.of(year, month, LocalDate.of(year, month, 1).lengthOfMonth());

        List<Mood> moods = moodRepository.findByUserAndDateBetween(user, startDate, endDate);

        for (Mood mood : moods) {
            moodDTOs.add(new MoodDTO(mood));
        }

        return ResponseEntity.ok(moodDTOs);
    }
}
