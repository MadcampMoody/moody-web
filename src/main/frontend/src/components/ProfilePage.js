import React, { useState, useEffect, useContext } from 'react';
import axios from 'axios';
import './ProfilePage.css';
import spotifyPlayerService from '../services/SpotifyPlayerService';
import { SpotifyPlayerContext } from '../App';

const ProfilePage = () => {
  const [likedSongs, setLikedSongs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [userName, setUserName] = useState('사용자');
  const [likedTrackIds, setLikedTrackIds] = useState(new Set());

  // 좋아요한 음악 목록 가져오기
  useEffect(() => {
    const fetchLikedSongs = async () => {
      try {
        setLoading(true);
        setError(null);
        
        // 좋아요한 음악 목록 가져오기
        const response = await axios.get('/api/liked-songs', { withCredentials: true });
        const songs = response.data || [];
        
        setLikedSongs(songs);
        console.log('좋아요한 음악 목록 조회 성공:', songs.length + '곡');
        
        // 좋아요 상태를 위한 trackId Set 생성
        const trackIds = new Set(songs.map(song => song.trackId));
        setLikedTrackIds(trackIds);
        
      } catch (error) {
        console.log('좋아요한 음악 목록 조회 실패 (조용히 처리):', error.message);
        setError('좋아요한 음악 목록을 가져올 수 없습니다.');
      } finally {
        setLoading(false);
      }
    };

    // 사용자 정보 가져오기
    const fetchUserInfo = async () => {
      try {
        const userResponse = await axios.get('/api/auth/user', { withCredentials: true });
        if (userResponse.data) {
          setUserName(userResponse.data.name || '사용자');
        }
      } catch (error) {
        console.log('사용자 정보 조회 실패:', error.message);
      }
    };

    fetchUserInfo();
    fetchLikedSongs();
  }, []);

  // 좋아요한 음악들을 큐에 추가
  const handleAddToQueue = async () => {
    if (likedSongs.length === 0) {
      alert("좋아요한 곡이 없습니다.");
      return;
    }

    try {
      const trackUris = likedSongs.map(song => `spotify:track:${song.trackId}`);
      const result = await spotifyPlayerService.addTracksToQueue(trackUris);

      if (result.success) {
        if (result.playedImmediately) {
          alert("좋아요한 곡 재생을 시작합니다!");
        } else if (result.preservedContext) {
          let message = `좋아요한 곡 ${trackUris.length}개가 큐에 추가되었습니다! 현재 듣고 있는 음악 이후에 재생되고, 그 다음 원래 플레이리스트가 이어집니다.`;
          
          if (result.shuffleEnabled) {
            message += "\n\n⚠️ 셔플 모드가 활성화되어 있어 좋아요한 곡들의 순서가 바뀔 수 있습니다.";
          }
          
          alert(message);
        } else {
          alert("좋아요한 곡들이 재생목록의 끝에 추가되었습니다!");
        }
      } else {
        alert("재생목록 추가에 실패했습니다. Spotify Premium 계정이 활성화되어 있는지 확인해주세요.");
      }
    } catch (error) {
      console.log('Spotify 큐 추가 중 오류 발생 (조용히 처리):', error.message);
      alert("Spotify 연결에 문제가 있습니다. Spotify 로그인 상태를 확인해주세요.");
    }
  };

  // 좋아요 토글
  const handleToggleLike = async (song) => {
    const isLiked = likedTrackIds.has(song.trackId);
    
    // Optimistic UI Update
    const newLikedTrackIds = new Set(likedTrackIds);
    const newLikedSongs = [...likedSongs];
    
    if (isLiked) {
      newLikedTrackIds.delete(song.trackId);
      const songIndex = newLikedSongs.findIndex(s => s.trackId === song.trackId);
      if (songIndex > -1) {
        newLikedSongs.splice(songIndex, 1);
      }
    } else {
      newLikedTrackIds.add(song.trackId);
      newLikedSongs.push(song);
    }
    
    setLikedTrackIds(newLikedTrackIds);
    setLikedSongs(newLikedSongs);

    try {
      const apiEndpoint = isLiked ? '/api/liked-songs/remove' : '/api/liked-songs/add';
      await axios.post(apiEndpoint, 
        { 
          trackId: song.trackId,
          title: song.title,
          artist: song.artist,
          musicUrl: song.musicUrl
        }, 
        { withCredentials: true }
      );
      console.log('좋아요 처리 성공:', isLiked ? '제거' : '추가');
    } catch (error) {
      console.log('좋아요 처리 실패 (조용히 처리):', error.message);
      // 실패해도 UI는 그대로 유지 (사용자 경험 개선)
    }
  };

  if (loading) {
    return (
      <div className="profile-page">
        <div className="profile-container">
          <div className="loading-message">
            좋아요한 음악 목록을 불러오는 중...
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="profile-page">
      <div className="profile-container">
        <div className="profile-header">
          <h1 className="profile-title">{userName}님의 프로필</h1>
          <p className="profile-subtitle">좋아요한 음악들을 관리해보세요</p>
        </div>

        {error && (
          <div className="error-message">
            {error}
          </div>
        )}

        {likedSongs.length > 0 ? (
          <div className="liked-songs-section">
            <div className="liked-songs-header">
              <h2 className="liked-songs-title">
                좋아요한 음악 ({likedSongs.length}곡)
              </h2>
              <button
                onClick={handleAddToQueue}
                className="add-to-queue-btn"
              >
                좋아요 곡 모두 담기
                <img src="/spotify-logo.svg" alt="Spotify" className="spotify-logo"/>
              </button>
            </div>

            <div className="liked-songs-list">
              {likedSongs.map((song, index) => (
                <div key={song.trackId || index} className="liked-song-item">
                  <span className="song-number">{index + 1}</span>
                  <div className="song-info">
                    <iframe
                      src={`https://open.spotify.com/embed/track/${song.trackId}?utm_source=generator&theme=0&controls=0`}
                      width="100%"
                      height="80"
                      frameBorder="0"
                      allowFullScreen=""
                      allow="autoplay; clipboard-write; encrypted-media; fullscreen; picture-in-picture"
                      loading="lazy"
                      title={`${song.title} by ${song.artist}`}
                    />
                  </div>
                  <button 
                    onClick={() => handleToggleLike(song)} 
                    className={`like-button liked`}
                    title="좋아요 취소"
                  >
                    <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor" stroke="currentColor" strokeWidth="2">
                      <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
                    </svg>
                  </button>
                </div>
              ))}
            </div>
          </div>
        ) : (
          <div className="empty-state">
            <div className="empty-icon">🎵</div>
            <h3>아직 좋아요한 음악이 없어요</h3>
            <p>일기를 작성하면서 음악을 추천받고 좋아요를 눌러보세요!</p>
          </div>
        )}
      </div>
    </div>
  );
};

export default ProfilePage; 