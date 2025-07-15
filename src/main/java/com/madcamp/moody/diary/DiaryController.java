package com.madcamp.moody.diary;

import com.madcamp.moody.user.User;
import com.madcamp.moody.user.UserRepository;
import com.madcamp.moody.mood.Mood;
import com.madcamp.moody.mood.MoodRepository;
import com.madcamp.moody.playlist.Playlist;
import com.madcamp.moody.playlist.PlaylistRepository;
import com.madcamp.moody.music.MusicRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/diary")
public class DiaryController {

    @Autowired
    private DiaryRepository diaryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MoodRepository moodRepository;

    @Autowired
    private PlaylistRepository playlistRepository;

    @Autowired
    private MusicRepository musicRepository;

    @PostMapping
    public ResponseEntity<Diary> saveDiary(
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

        Diary savedDiary = diaryRepository.save(diary);

        return ResponseEntity.ok(savedDiary);
    }

    @Transactional
    @PostMapping("/delete")
    public ResponseEntity<?> deleteDiaryAndAssociations(@RequestBody Map<String, Long> body) {
        Long diaryId = body.get("diaryId");
        Diary diary = diaryRepository.findById(diaryId).orElse(null);

        if (diary == null) {
            return ResponseEntity.status(404).body("Diary not found");
        }

        // 1. 다이어리와 연관된 플레이리스트 찾기
        List<Playlist> playlists = playlistRepository.findByDiaryId(diaryId);

        for (Playlist playlist : playlists) {
            // 2. 각 플레이리스트에 속한 모든 음악 삭제
            musicRepository.deleteByPlaylistId(playlist.getPlaylistId());
        }

        // 3. 플레이리스트 모두 삭제
        playlistRepository.deleteAll(playlists);

        // 4. 다이어리와 연관된 기분(Mood) 삭제
        Mood mood = diary.getMood();
        if (mood != null) {
            moodRepository.delete(mood);
        }

        // 5. 다이어리 삭제
        diaryRepository.delete(diary);

        return ResponseEntity.ok("Diary and all associated data have been deleted.");
    }
}
