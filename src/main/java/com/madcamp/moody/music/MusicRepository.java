package com.madcamp.moody.music;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MusicRepository extends JpaRepository<Music, Long> {

    // playlist_id로 music 찾기
    List<Music> findByPlaylistId(Long playlistId);

    // music_url로 music 찾기 (부분 검색)
    List<Music> findByMusicUrlContainingIgnoreCase(String musicUrl);

    // playlist_id와 music_url로 music 찾기
    List<Music> findByPlaylistIdAndMusicUrlContainingIgnoreCase(Long playlistId, String musicUrl);

    // 특정 playlist_id에 속한 music 개수 조회
    long countByPlaylistId(Long playlistId);

    // 특정 playlist_id에 속한 music를 music_id 기준으로 정렬해서 조회
    @Query("SELECT m FROM Music m WHERE m.playlistId = :playlistId ORDER BY m.musicId ASC")
    List<Music> findByPlaylistIdOrderByMusicIdAsc(@Param("playlistId") Long playlistId);

    // 특정 music_url과 playlist_id로 music 존재 여부 확인
    boolean existsByMusicUrlAndPlaylistId(String musicUrl, Long playlistId);

    // music_id와 playlist_id로 찾기 (소유권 확인용)
    Optional<Music> findByMusicIdAndPlaylistId(Long musicId, Long playlistId);

    // 특정 playlist_id에 속한 모든 music 삭제
    void deleteByPlaylistId(Long playlistId);
} 