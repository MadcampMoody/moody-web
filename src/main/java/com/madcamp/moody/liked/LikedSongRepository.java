package com.madcamp.moody.liked;

import com.madcamp.moody.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface LikedSongRepository extends JpaRepository<LikedSong, Long> {
    // 사용자가 '좋아요'한 모든 노래 조회
    List<LikedSong> findByUser(User user);

    // 사용자가 특정 노래를 '좋아요'했는지 확인
    Optional<LikedSong> findByUserAndTrackId(User user, String trackId);

    // 사용자와 트랙 ID로 존재 여부 확인 (더 효율적)
    boolean existsByUserAndTrackId(User user, String trackId);
    
    // '좋아요' 취소
    void deleteByUserAndTrackId(User user, String trackId);
} 