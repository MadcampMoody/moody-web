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
        // 1. 로그인한 사용자 찾기
        String oauthId = oauth2User.getAttribute("id").toString();
        User user = userRepository.findByOauthId(oauthId);

        // 2. 감정 enum 변환
        MoodType moodType = MoodType.valueOf(request.getMood());

        // 3. Mood 엔티티 생성 및 저장
        Mood mood = new Mood();
        mood.setUser(user);
        mood.setMoodType(moodType);
        mood.setDate(LocalDate.parse(request.getDate()));

        moodRepository.save(mood);

        return ResponseEntity.ok().body("감정 기록 완료!");
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
        result.put("diary", diary != null ? diary.getContent() : "");

        return ResponseEntity.ok(result);
    }
}
