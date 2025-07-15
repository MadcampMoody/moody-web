import React, { useState, useEffect, useRef } from 'react';
import './SpotifyPlayer.css';

const SpotifyPlayer = () => {
  const [player, setPlayer] = useState(null);
  const [deviceId, setDeviceId] = useState(null);
  const [currentTrack, setCurrentTrack] = useState(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const [position, setPosition] = useState(0);
  const [duration, setDuration] = useState(0);
  const [volume, setVolume] = useState(0.5);
  const [accessToken, setAccessToken] = useState(null);
  const [isPaused, setIsPaused] = useState(true);
  const [canControl, setCanControl] = useState(false);
  const [isReady, setIsReady] = useState(false);
  const playerRef = useRef(null);
  const trackNameRef = useRef(null);
  const artistNameRef = useRef(null);

  // Spotify 액세스 토큰 가져오기 (카카오와 완전 분리)
  const getSpotifyAccessToken = async () => {
    try {
      console.log('Spotify 액세스 토큰 요청 시작...');
      const response = await fetch('http://127.0.0.1:8080/api/spotify/access-token', {
        method: 'GET',
        credentials: 'include',
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json',
        },
      });
      
      console.log('토큰 응답 상태:', response.status);
      
      if (response.ok) {
        const data = await response.json();
        console.log('토큰 조회 성공:', data.success);
        return data.accessToken;
      } else {
        const errorData = await response.json().catch(() => ({}));
        console.error('Spotify 액세스 토큰 가져오기 실패:', response.status, errorData);
        return null;
      }
    } catch (error) {
      console.error('Spotify 액세스 토큰 요청 오류:', error);
      return null;
    }
  };

  // 이 디바이스로 재생 전환
  const transferPlaybackToThisDevice = async (deviceId) => {
    if (!accessToken) return;
    
    try {
      console.log('재생을 이 디바이스로 전환 중...', deviceId);
      
      const response = await fetch('https://api.spotify.com/v1/me/player', {
        method: 'PUT',
        headers: {
          'Authorization': `Bearer ${accessToken}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          device_ids: [deviceId],
          play: false // 자동 재생하지 않음
        })
      });

      if (response.status === 204) {
        console.log('재생 디바이스 전환 완료');
        // 잠시 후 현재 재생 상태 확인
        setTimeout(() => {
          checkCurrentPlaybackState();
        }, 1000);
      } else {
        console.log('재생 디바이스 전환 응답:', response.status);
      }
    } catch (error) {
      console.error('재생 디바이스 전환 오류:', error);
    }
  };

  // 현재 재생 상태 확인
  const checkCurrentPlaybackState = async () => {
    if (!accessToken) return;
    
    try {
      const response = await fetch('https://api.spotify.com/v1/me/player', {
        headers: {
          'Authorization': `Bearer ${accessToken}`,
        },
      });

      if (response.ok) {
        const data = await response.json();
        console.log('현재 재생 상태:', data);
        
        if (data && data.item) {
          setCurrentTrack(data.item);
          setIsPlaying(data.is_playing);
          setIsPaused(!data.is_playing);
          setCanControl(true);
          setPosition(data.progress_ms || 0);
          setDuration(data.item.duration_ms || 0);
        }
      } else if (response.status === 204) {
        console.log('현재 재생 중인 곡이 없습니다');
      }
    } catch (error) {
      console.error('재생 상태 확인 오류:', error);
    }
  };

  // Spotify SDK 동적 로드
  const loadSpotifySDK = () => {
    return new Promise((resolve, reject) => {
      // 이미 로드되어 있다면 바로 resolve
      if (window.Spotify && window.Spotify.Player) {
        console.log('Spotify SDK 이미 로드됨');
        resolve(window.Spotify);
        return;
      }

      console.log('Spotify SDK 로딩 시작...');
      
      // 스크립트가 이미 존재하는지 확인
      const existingScript = document.querySelector('script[src="https://sdk.scdn.co/spotify-player.js"]');
      if (existingScript) {
        // 이미 스크립트가 있다면 로딩 완료를 기다림
        if (window.Spotify && window.Spotify.Player) {
          resolve(window.Spotify);
        } else {
          window.onSpotifyWebPlaybackSDKReady = () => {
            console.log('Spotify SDK 로드 완료');
            resolve(window.Spotify);
          };
        }
        return;
      }

      const script = document.createElement('script');
      script.src = 'https://sdk.scdn.co/spotify-player.js';
      script.async = true;
      
      // SDK 로드 완료 콜백 먼저 설정
      window.onSpotifyWebPlaybackSDKReady = () => {
        console.log('Spotify SDK 로드 완료');
        resolve(window.Spotify);
      };
      
      script.onload = () => {
        console.log('Spotify SDK 스크립트 로드됨');
        // onSpotifyWebPlaybackSDKReady 콜백이 호출될 때까지 대기
      };
      
      script.onerror = () => {
        console.error('Spotify SDK 스크립트 로드 실패');
        reject(new Error('Spotify SDK 로드 실패'));
      };
      
      document.head.appendChild(script);
      
      // 타임아웃 설정 (15초로 증가)
      setTimeout(() => {
        if (!window.Spotify || !window.Spotify.Player) {
          console.error('Spotify SDK 로드 타임아웃');
          reject(new Error('Spotify SDK 로드 타임아웃'));
        }
      }, 15000);
    });
  };

  // Spotify Web Playback SDK 초기화
  useEffect(() => {
    const initializePlayer = async () => {
      try {
        const token = await getSpotifyAccessToken();
        if (!token) {
          console.log('Spotify 액세스 토큰이 없습니다. 플레이어를 초기화하지 않습니다.');
          return;
        }
        
        setAccessToken(token);

        // SDK 로드
        await loadSpotifySDK();

        console.log('Spotify Web Playback SDK 초기화 시작...');
        
        const spotifyPlayer = new window.Spotify.Player({
          name: 'Moody Web Player',
          getOAuthToken: cb => { cb(token); },
          volume: 0.5
        });

        playerRef.current = spotifyPlayer;

        // 플레이어 이벤트 리스너 설정
        spotifyPlayer.addListener('ready', ({ device_id }) => {
          console.log('Ready with Device ID', device_id);
          setDeviceId(device_id);
          setIsReady(true); // 플레이어가 준비되면 준비 상태 업데이트
          transferPlaybackToThisDevice(device_id); // 디바이스가 준비되면 자동 재생 전환
        });

        spotifyPlayer.addListener('not_ready', ({ device_id }) => {
          console.log('Device ID has gone offline', device_id);
          setIsReady(false); // 디바이스가 오프라인이 되면 준비 상태 업데이트
        });

        spotifyPlayer.addListener('player_state_changed', (state) => {
          console.log('Player state changed:', state);
          
          if (!state) {
            setCanControl(false);
            setCurrentTrack(null);
            setIsPlaying(false);
            setIsPaused(true);
            return;
          }

          // 재생 가능한 곡이 있는지 확인
          const hasTrack = state.track_window && state.track_window.current_track;
          setCanControl(hasTrack);
          
          if (hasTrack) {
            setCurrentTrack(state.track_window.current_track);
            setIsPlaying(!state.paused);
            setIsPaused(state.paused);
            setPosition(state.position);
            setDuration(state.duration);
          } else {
            setCurrentTrack(null);
            setIsPlaying(false);
            setIsPaused(true);
            setPosition(0);
            setDuration(0);
          }
        });

        spotifyPlayer.addListener('initialization_error', ({ message }) => {
          console.error('Failed to initialize:', message);
        });

        spotifyPlayer.addListener('authentication_error', ({ message }) => {
          console.error('Failed to authenticate:', message);
        });

        spotifyPlayer.addListener('account_error', ({ message }) => {
          console.error('Failed to validate Spotify account:', message);
        });

        spotifyPlayer.addListener('playback_error', ({ message }) => {
          console.error('Failed to perform playback:', message);
        });

        // 플레이어 연결
        spotifyPlayer.connect().then(success => {
          if (success) {
            console.log('Spotify Player 연결 성공!');
            setPlayer(spotifyPlayer);
          } else {
            console.error('Spotify Player 연결 실패');
          }
        }).catch(error => {
          console.error('Spotify Player 연결 오류:', error);
        });

      } catch (error) {
        console.error('Spotify Player 초기화 중 전체 오류:', error);
      }
    };

    initializePlayer();

    // 컴포넌트 언마운트 시 플레이어 연결 해제
    return () => {
      if (playerRef.current) {
        playerRef.current.disconnect();
      }
    };
  }, []);

  // 재생/일시정지
  const togglePlayPause = async () => {
    if (!player || !isReady) {
      console.warn('플레이어가 준비되지 않았습니다.');
      return;
    }
    
    try {
      if (!canControl || !currentTrack) {
        // 재생할 곡이 없으면 웹 플레이어에서 재생 시작
        console.log('재생할 곡이 없습니다. 최근 재생한 곡을 이 디바이스에서 시작합니다...');
        await startPlaybackOnWebPlayer();
      } else {
        // 재생 중인 곡이 있으면 Web Playback SDK로 토글
        await player.togglePlay();
      }
    } catch (error) {
      console.error('재생/일시정지 오류:', error);
    }
  };

  // 웹 플레이어에서 직접 재생 시작
  const startPlaybackOnWebPlayer = async () => {
    if (!deviceId || !accessToken) {
      console.warn('디바이스 ID 또는 액세스 토큰이 없습니다.');
      return;
    }
    
    try {
      console.log('웹 플레이어에서 재생 시작 중...', deviceId);
      
      // 최근 재생한 곡들 가져오기
      const recentResponse = await fetch('https://api.spotify.com/v1/me/player/recently-played?limit=1', {
        headers: {
          'Authorization': `Bearer ${accessToken}`,
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

      const playResponse = await fetch(`https://api.spotify.com/v1/me/player/play?device_id=${deviceId}`, {
        method: 'PUT',
        headers: {
          'Authorization': `Bearer ${accessToken}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(playData)
      });

      if (playResponse.status === 204) {
        console.log('웹 플레이어에서 재생 시작 성공');
        // 잠시 후 플레이어 상태가 업데이트될 것임
        setTimeout(() => {
          checkCurrentPlaybackState();
        }, 500);
      } else {
        console.error('웹 플레이어 재생 시작 실패:', playResponse.status);
        const errorText = await playResponse.text();
        console.error('오류 응답:', errorText);
        
        let errorMessage = '재생 시작에 실패했습니다.';
        if (playResponse.status === 403) {
          errorMessage = 'Spotify Premium 계정이 필요합니다.';
        } else if (playResponse.status === 401) {
          errorMessage = 'Spotify 인증이 만료되었습니다. 다시 로그인해주세요.';
        }
        alert(errorMessage);
      }
    } catch (error) {
      console.error('웹 플레이어 재생 시작 오류:', error);
      alert('재생 시작 중 오류가 발생했습니다.');
    }
  };

  // 이전 곡
  const previousTrack = async () => {
    if (!player || !isReady || !canControl) {
      console.warn('플레이어가 준비되지 않았거나 재생할 곡이 없습니다.');
      return;
    }
    
    try {
      await player.previousTrack();
    } catch (error) {
      console.error('이전 곡 오류:', error);
      alert('이전 곡으로 이동할 수 없습니다.');
    }
  };

  // 다음 곡
  const nextTrack = async () => {
    if (!player || !isReady || !canControl) {
      console.warn('플레이어가 준비되지 않았거나 재생할 곡이 없습니다.');
      return;
    }
    
    try {
      await player.nextTrack();
    } catch (error) {
      console.error('다음 곡 오류:', error);
      if (error.message.includes('no list was loaded')) {
        alert('재생할 곡이 없습니다. Spotify 앱에서 먼저 음악을 재생해주세요.');
      }
    }
  };



  // 시간 포맷팅
  const formatTime = (ms) => {
    const minutes = Math.floor(ms / 60000);
    const seconds = Math.floor((ms % 60000) / 1000);
    return `${minutes}:${seconds.toString().padStart(2, '0')}`;
  };

  // 진행바 클릭으로 재생 위치 이동
  const handleProgressBarClick = async (event) => {
    if (!player || !canControl || !duration) {
      console.warn('플레이어가 준비되지 않았거나 재생할 곡이 없습니다.');
      return;
    }

    try {
      const progressBar = event.currentTarget;
      const rect = progressBar.getBoundingClientRect();
      const clickX = event.clientX - rect.left;
      const progressBarWidth = rect.width;
      const clickPercentage = clickX / progressBarWidth;
      const seekPosition = Math.floor(duration * clickPercentage);

      console.log('진행바 클릭:', {
        clickX,
        progressBarWidth,
        clickPercentage,
        seekPosition,
        duration
      });

      // Spotify Web Playback SDK의 seek 기능 사용
      await player.seek(seekPosition);
      
      // 위치 즉시 업데이트 (실제 위치는 player_state_changed 이벤트에서 업데이트됨)
      setPosition(seekPosition);
      
    } catch (error) {
      console.error('재생 위치 이동 오류:', error);
      alert('재생 위치를 이동할 수 없습니다.');
    }
  };

  // 텍스트 길이에 따라 스크롤 애니메이션 클래스 적용
  useEffect(() => {
    const checkTextOverflow = () => {
      if (currentTrack) {
        // 트랙 이름 길이 확인
        if (trackNameRef.current) {
          const element = trackNameRef.current;
          // 애니메이션을 일시적으로 제거하여 정확한 크기 측정
          element.classList.remove('scrolling');
          
                     setTimeout(() => {
             const isOverflowing = element.scrollWidth > element.clientWidth;
             console.log('Track name overflow check:', {
               scrollWidth: element.scrollWidth,
               clientWidth: element.clientWidth,
               isOverflowing: isOverflowing,
               text: element.textContent
             });
             
             if (isOverflowing) {
               element.classList.add('scrolling');
               // 동적으로 CSS 변수 설정
               element.style.setProperty('--scroll-width', `${element.clientWidth}px`);
             }
           }, 10);
        }

        // 아티스트 이름 길이 확인
        if (artistNameRef.current) {
          const element = artistNameRef.current;
          // 애니메이션을 일시적으로 제거하여 정확한 크기 측정
          element.classList.remove('scrolling');
          
                     setTimeout(() => {
             const isOverflowing = element.scrollWidth > element.clientWidth;
             console.log('Artist name overflow check:', {
               scrollWidth: element.scrollWidth,
               clientWidth: element.clientWidth,
               isOverflowing: isOverflowing,
               text: element.textContent
             });
             
             if (isOverflowing) {
               element.classList.add('scrolling');
               // 동적으로 CSS 변수 설정
               element.style.setProperty('--scroll-width', `${element.clientWidth}px`);
             }
           }, 10);
        }
      }
    };

    checkTextOverflow();
    
    // 약간의 지연 후 다시 한번 체크 (렌더링이 완전히 끝난 후)
    const timer = setTimeout(checkTextOverflow, 100);
    
    return () => clearTimeout(timer);
  }, [currentTrack]);

  // 토큰이 없거나 플레이어가 없으면 로딩 표시
  if (!accessToken) {
    return (
      <div className="spotify-player loading">
        <div className="loading-text">Spotify 인증 확인 중...</div>
      </div>
    );
  }

  if (!player || !isReady) {
    return (
      <div className="spotify-player loading">
        <div className="loading-text">
          {!player ? 'Spotify 플레이어 초기화 중...' : '웹 플레이어 준비 중...'}
        </div>
      </div>
    );
  }

  return (
    <div className="spotify-player">
      {currentTrack ? (
        <div className="now-playing">
          <div className="track-info">
            <img 
              src={currentTrack.album.images[0]?.url} 
              alt={currentTrack.album.name}
              className="album-cover"
            />
            <div className="track-details">
              <div className="track-name" ref={trackNameRef}>{currentTrack.name}</div>
              <div className="artist-name" ref={artistNameRef}>{currentTrack.artists.map(artist => artist.name).join(', ')}</div>
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
          className={`control-btn ${!canControl ? 'disabled' : ''}`} 
          onClick={previousTrack}
          disabled={!canControl}
          title={!canControl ? "재생 중인 곡이 없습니다" : "이전 곡"}
        >
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
            <path d="M6 6h2v12H6zm3.5 6l8.5 6V6z" fill="currentColor"/>
          </svg>
        </button>
        
        <button 
          className="control-btn play-pause" 
          onClick={togglePlayPause}
          title={!canControl ? "재생 시작" : (isPlaying ? "일시정지" : "재생")}
        >
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
            {isPlaying ? (
              <path d="M6 4h4v16H6V4zm8 0h4v16h-4V4z" fill="currentColor"/>
            ) : (
              <path d="M8 5v14l11-7z" fill="currentColor"/>
            )}
          </svg>
        </button>
        
        <button 
          className={`control-btn ${!canControl ? 'disabled' : ''}`} 
          onClick={nextTrack}
          disabled={!canControl}
          title={!canControl ? "재생 중인 곡이 없습니다" : "다음 곡"}
        >
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
            <path d="M6 18l8.5-6L6 6v12zM16 6v12h2V6h-2z" fill="currentColor"/>
          </svg>
        </button>
      </div>

      <div className="progress-section">
        <span className="time-display">{formatTime(position)}</span>
        <div 
          className="progress-bar" 
          onClick={handleProgressBarClick}
          title={canControl ? "클릭하여 재생 위치 이동" : "재생 중인 곡이 없습니다"}
        >
          <div 
            className="progress-fill" 
            style={{ width: `${duration ? (position / duration) * 100 : 0}%` }}
          ></div>
        </div>
        <span className="time-display">{formatTime(duration)}</span>
      </div>

    </div>
  );
};

export default SpotifyPlayer; 