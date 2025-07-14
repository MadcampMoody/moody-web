package com.madcamp.moody.playlist;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlaylistRepository extends JpaRepository<Playlist, Long> {

    // diary_id로 playlist 찾기
    List<Playlist> findByDiaryId(Long diaryId);

    // title로 playlist 찾기 (부분 검색)
    List<Playlist> findByTitleContainingIgnoreCase(String title);

    // diary_id와 title로 playlist 찾기
    List<Playlist> findByDiaryIdAndTitleContainingIgnoreCase(Long diaryId, String title);

    // 특정 diary_id에 속한 playlist 개수 조회
    long countByDiaryId(Long diaryId);

    // 특정 diary_id에 속한 playlist를 생성일 기준으로 정렬해서 조회
    @Query("SELECT p FROM Playlist p WHERE p.diaryId = :diaryId ORDER BY p.createdAt DESC")
    List<Playlist> findByDiaryIdOrderByCreatedAtDesc(@Param("diaryId") Long diaryId);

    // 특정 title과 diary_id로 playlist 존재 여부 확인
    boolean existsByTitleAndDiaryId(String title, Long diaryId);

    // playlist_id와 diary_id로 찾기 (소유권 확인용)
    Optional<Playlist> findByPlaylistIdAndDiaryId(Long playlistId, Long diaryId);
    
    // 사용자 ID와 날짜로 플레이리스트 찾기
    List<Playlist> findByDiaryIdAndDate(Long diaryId, java.time.LocalDate date);
} 