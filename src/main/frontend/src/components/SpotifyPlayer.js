import React, { useState, useEffect, useRef } from 'react';
import './SpotifyPlayer.css';
import spotifyPlayerService from '../services/SpotifyPlayerService';

const SpotifyPlayer = () => {
  const [playerState, setPlayerState] = useState(spotifyPlayerService.getState());
  const trackNameRef = useRef(null);
  const artistNameRef = useRef(null);

  // 전역 서비스 상태 변경 리스너 등록
  useEffect(() => {
    const unsubscribe = spotifyPlayerService.addStateListener((newState) => {
      setPlayerState(newState);
    });

    // 초기 상태 설정
    setPlayerState(spotifyPlayerService.getState());

    return unsubscribe;
  }, []);

  // 플레이어 초기화 (아직 초기화되지 않은 경우)
  useEffect(() => {
    const initializeIfNeeded = async () => {
      if (!playerState.player && !spotifyPlayerService.sdkLoading) {
        await spotifyPlayerService.initializePlayer();
      }
    };

    initializeIfNeeded();
  }, [playerState.player]);

  // 재생/일시정지
  const togglePlayPause = async () => {
    if (!playerState.player || !playerState.isReady) {
      console.warn('플레이어가 준비되지 않았습니다.');
      return;
    }
    
    try {
      if (!playerState.canControl || !playerState.currentTrack) {
        // 재생할 곡이 없으면 웹 플레이어에서 재생 시작
        console.log('재생할 곡이 없습니다. 최근 재생한 곡을 이 디바이스에서 시작합니다...');
        await startPlaybackOnWebPlayer();
      } else {
        // 재생 중인 곡이 있으면 Web Playback SDK로 토글
        await playerState.player.togglePlay();
      }
    } catch (error) {
      console.error('재생/일시정지 오류:', error);
    }
  };

  // 웹 플레이어에서 직접 재생 시작
  const startPlaybackOnWebPlayer = async () => {
    if (!playerState.deviceId || !playerState.accessToken) {
      console.warn('디바이스 ID 또는 액세스 토큰이 없습니다.');
      return;
    }
    
    try {
      console.log('웹 플레이어에서 재생 시작 중...', playerState.deviceId);
      
      // 최근 재생한 곡들 가져오기
      const recentResponse = await fetch('https://api.spotify.com/v1/me/player/recently-played?limit=1', {
        headers: {
          'Authorization': `Bearer ${playerState.accessToken}`,
        },
      });

      let trackUri = null;
      if (recentResponse.ok) {
        const recentData = await recentResponse.json();
        if (recentData.items && recentData.items.length > 0) {
          trackUri = recentData.items[0].track.uri;
          console.log('최근 재생한 곡 발견:', trackUri);
        }
      }

      // 웹 플레이어 디바이스에서 재생 시작
      const playData = {};
      if (trackUri) {
        playData.uris = [trackUri];
      }

      const playResponse = await fetch(`https://api.spotify.com/v1/me/player/play?device_id=${playerState.deviceId}`, {
        method: 'PUT',
        headers: {
          'Authorization': `Bearer ${playerState.accessToken}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(playData)
      });

      // 204는 성공, 202도 성공 응답으로 처리
      if (playResponse.status === 204 || playResponse.status === 202) {
        console.log('웹 플레이어에서 재생 시작 성공 (상태 코드:', playResponse.status, ')');
        setTimeout(() => {
          spotifyPlayerService.checkCurrentPlaybackState();
        }, 500);
      } else if (playResponse.status >= 400) {
        // 400번대, 500번대 오류만 사용자에게 표시
        console.error('웹 플레이어 재생 시작 실패:', playResponse.status);
        const errorText = await playResponse.text();
        console.error('오류 응답:', errorText);
        
        let errorMessage = '재생 시작에 실패했습니다.';
        if (playResponse.status === 403) {
          errorMessage = 'Spotify Premium 계정이 필요합니다.';
        } else if (playResponse.status === 401) {
          errorMessage = 'Spotify 인증이 만료되었습니다. 다시 로그인해주세요.';
        } else if (playResponse.status === 404) {
          errorMessage = '재생할 수 있는 디바이스를 찾을 수 없습니다.';
        }
        alert(errorMessage);
      } else {
        // 200번대 응답은 성공으로 처리하되 로그만 남김
        console.log('웹 플레이어 재생 요청 완료 (상태 코드:', playResponse.status, ')');
        setTimeout(() => {
          spotifyPlayerService.checkCurrentPlaybackState();
        }, 500);
      }
    } catch (error) {
      console.error('웹 플레이어 재생 시작 오류:', error);
      
      // 네트워크 오류나 심각한 오류만 사용자에게 표시
      if (error.name === 'TypeError' || error.message.includes('fetch')) {
        alert('네트워크 연결에 문제가 있습니다. 인터넷 연결을 확인해주세요.');
      } else if (error.message.includes('Premium')) {
        alert('Spotify Premium 계정이 필요합니다.');
      } else {
        // 일반적인 오류는 콘솔에만 기록하고 사용자에게는 표시하지 않음
        console.log('재생 시작 중 일반 오류 발생, 사용자에게 표시하지 않음');
      }
    }
  };

  // 이전 곡
  const previousTrack = async () => {
    if (!playerState.player || !playerState.isReady || !playerState.canControl) {
      console.warn('플레이어가 준비되지 않았거나 재생할 곡이 없습니다.');
      return;
    }
    
    try {
      await playerState.player.previousTrack();
    } catch (error) {
      console.error('이전 곡 오류:', error);
      alert('이전 곡으로 이동할 수 없습니다.');
    }
  };

  // 다음 곡
  const nextTrack = async () => {
    if (!playerState.player || !playerState.isReady || !playerState.canControl) {
      console.warn('플레이어가 준비되지 않았거나 재생할 곡이 없습니다.');
      return;
    }
    
    try {
      await playerState.player.nextTrack();
    } catch (error) {
      console.error('다음 곡 오류:', error);
      alert('다음 곡으로 이동할 수 없습니다.');
    }
  };

  // 진행바 클릭으로 재생 위치 이동
  const handleProgressBarClick = async (event) => {
    if (!playerState.player || !playerState.canControl || !playerState.duration) {
      console.warn('플레이어가 준비되지 않았거나 재생할 곡이 없습니다.');
      return;
    }

    try {
      const progressBar = event.currentTarget;
      const rect = progressBar.getBoundingClientRect();
      const clickX = event.clientX - rect.left;
      const progressBarWidth = rect.width;
      const clickPercentage = clickX / progressBarWidth;
      const seekPosition = Math.floor(playerState.duration * clickPercentage);

      console.log('진행바 클릭:', {
        clickX,
        progressBarWidth,
        clickPercentage,
        seekPosition,
        duration: playerState.duration
      });

      // Spotify Web Playback SDK의 seek 기능 사용
      await playerState.player.seek(seekPosition);
      
    } catch (error) {
      console.error('재생 위치 이동 오류:', error);
      alert('재생 위치를 이동할 수 없습니다.');
    }
  };

  // 시간 포맷팅
  const formatTime = (ms) => {
    const minutes = Math.floor(ms / 60000);
    const seconds = Math.floor((ms % 60000) / 1000);
    return `${minutes}:${seconds.toString().padStart(2, '0')}`;
  };

  // 남은 시간 포맷팅 (음수 표시)
  const formatRemainingTime = (currentMs, totalMs) => {
    const remainingMs = totalMs - currentMs;
    const minutes = Math.floor(remainingMs / 60000);
    const seconds = Math.floor((remainingMs % 60000) / 1000);
    return `-${minutes}:${seconds.toString().padStart(2, '0')}`;
  };

  // 텍스트 길이에 따라 스크롤 애니메이션 클래스 적용
  useEffect(() => {
    const checkTextOverflow = () => {
      if (playerState.currentTrack) {
        // 트랙 이름 길이 확인
        if (trackNameRef.current) {
          const element = trackNameRef.current;
          element.classList.remove('scrolling');
          
          setTimeout(() => {
            const isOverflowing = element.scrollWidth > element.clientWidth;
            if (isOverflowing) {
              element.classList.add('scrolling');
              element.style.setProperty('--scroll-width', `${element.clientWidth}px`);
            }
          }, 10);
        }

        // 아티스트 이름 길이 확인
        if (artistNameRef.current) {
          const element = artistNameRef.current;
          element.classList.remove('scrolling');
          
          setTimeout(() => {
            const isOverflowing = element.scrollWidth > element.clientWidth;
            if (isOverflowing) {
              element.classList.add('scrolling');
              element.style.setProperty('--scroll-width', `${element.clientWidth}px`);
            }
          }, 10);
        }
      }
    };

    checkTextOverflow();
    
    const timer = setTimeout(checkTextOverflow, 100);
    
    return () => clearTimeout(timer);
  }, [playerState.currentTrack]);

  // 토큰이 없거나 플레이어가 없으면 로딩 표시
  if (!playerState.accessToken) {
    return (
      <div className="spotify-player loading">
        <div className="loading-text">Spotify 인증 확인 중...</div>
      </div>
    );
  }

  if (!playerState.player || !playerState.isReady) {
    return (
      <div className="spotify-player loading">
        <div className="loading-text">
          {!playerState.player ? 'Spotify 플레이어 초기화 중...' : '웹 플레이어 준비 중...'}
        </div>
      </div>
    );
  }

  return (
    <div className="spotify-player">
      {playerState.currentTrack ? (
        <div className="now-playing">
          <div className="track-info">
            <img 
              src={playerState.currentTrack.album.images[0]?.url} 
              alt={playerState.currentTrack.album.name}
              className="album-cover"
            />
            <div className="track-details">
              <div className="track-name" ref={trackNameRef}>{playerState.currentTrack.name}</div>
              <div className="artist-name" ref={artistNameRef}>{playerState.currentTrack.artists.map(artist => artist.name).join(', ')}</div>
            </div>
          </div>
        </div>
      ) : (
        <div className="now-playing">
          <div className="track-info">
            <div className="no-track-placeholder">
              <svg width="48" height="48" viewBox="0 0 24 24" fill="none">
                <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z" fill="rgba(255,255,255,0.5)"/>
              </svg>
            </div>
            <div className="track-details">
              <div className="track-name">재생 중인 곡이 없습니다</div>
              <div className="artist-name">재생 버튼을 클릭하여 음악을 시작하세요</div>
            </div>
          </div>
        </div>
      )}

      <div className="player-controls">
        <button 
          className={`control-btn ${!playerState.canControl ? 'disabled' : ''}`} 
          onClick={previousTrack}
          disabled={!playerState.canControl}
          title={!playerState.canControl ? "재생 중인 곡이 없습니다" : "이전 곡"}
        >
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
            <path d="M6 6h2v12H6zm3.5 6l8.5 6V6z" fill="currentColor"/>
          </svg>
        </button>
        
        <button 
          className="control-btn play-pause" 
          onClick={togglePlayPause}
          title={!playerState.canControl ? "재생 시작" : (playerState.isPlaying ? "일시정지" : "재생")}
        >
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
            {playerState.isPlaying ? (
              <path d="M6 4h4v16H6V4zm8 0h4v16h-4V4z" fill="currentColor"/>
            ) : (
              <path d="M8 5v14l11-7z" fill="currentColor"/>
            )}
          </svg>
        </button>
        
        <button 
          className={`control-btn ${!playerState.canControl ? 'disabled' : ''}`} 
          onClick={nextTrack}
          disabled={!playerState.canControl}
          title={!playerState.canControl ? "재생 중인 곡이 없습니다" : "다음 곡"}
        >
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
            <path d="M6 18l8.5-6L6 6v12zM16 6v12h2V6h-2z" fill="currentColor"/>
          </svg>
        </button>
      </div>

      <div className="progress-section">
        <span className="time-display">{formatTime(playerState.position)}</span>
        <div 
          className="progress-bar" 
          onClick={handleProgressBarClick}
          title={playerState.canControl ? "클릭하여 재생 위치 이동" : "재생 중인 곡이 없습니다"}
        >
          <div 
            className="progress-fill" 
            style={{ width: `${playerState.duration ? (playerState.position / playerState.duration) * 100 : 0}%` }}
          ></div>
        </div>
        <span className="time-display">
          {playerState.duration ? formatRemainingTime(playerState.position, playerState.duration) : '-0:00'}
        </span>
      </div>

    </div>
  );
};

export default SpotifyPlayer; 