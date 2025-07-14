package com.madcamp.moody.diary;

import com.madcamp.moody.user.User;
import com.madcamp.moody.user.UserRepository;
import com.madcamp.moody.mood.Mood;
import com.madcamp.moody.mood.MoodRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/diary")
public class DiaryController {

    @Autowired
    private DiaryRepository diaryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MoodRepository moodRepository;

    @PostMapping
    public ResponseEntity<?> saveDiary(
            @RequestBody DiaryRequestDTO request,
            @AuthenticationPrincipal OAuth2User oauth2User
    ) {
        String oauthId = oauth2User.getAttribute("id").toString();
        User user = userRepository.findByOauthId(oauthId);

        // 해당 날짜의 Mood 찾기
        Mood mood = moodRepository.findByUserAndDate(user, LocalDate.parse(request.getDate()));

        Diary diary = new Diary();
        diary.setUser(user);
        diary.setMood(mood);
        diary.setContent(request.getContent());

        diaryRepository.save(diary);

        return ResponseEntity.ok().body("일기 저장 완료!");
    }
}
