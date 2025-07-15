// Spotify Player를 전역에서 관리하는 서비스
class SpotifyPlayerService {
  constructor() {
    this.player = null;
    this.deviceId = null;
    this.accessToken = null;
    this.isReady = false;
    this.currentTrack = null;
    this.isPlaying = false;
    this.position = 0;
    this.duration = 0;
    this.isPaused = true;
    this.canControl = false;
    this.volume = 0.5;
    
    // 상태 변경 리스너들
    this.listeners = new Set();
    
    // SDK 로딩 상태
    this.sdkLoaded = false;
    this.sdkLoading = false;
    
    // Position 업데이트를 위한 interval
    this.positionUpdateInterval = null;
    this.lastPositionUpdate = Date.now();
  }

  // 리스너 등록
  addStateListener(listener) {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  }

  // 실시간 position 업데이트 시작
  startPositionUpdate() {
    if (this.positionUpdateInterval) {
      clearInterval(this.positionUpdateInterval);
    }
    
    this.positionUpdateInterval = setInterval(() => {
      if (this.isPlaying && this.currentTrack && this.duration > 0) {
        const now = Date.now();
        const timeDiff = now - this.lastPositionUpdate;
        
        // 1초 이상 차이가 나면 position 업데이트
        if (timeDiff >= 1000) {
          this.position = Math.min(this.position + timeDiff, this.duration);
          this.lastPositionUpdate = now;
          this.notifyStateChange();
        }
      }
    }, 1000);
  }

  // Position 업데이트 중지
  stopPositionUpdate() {
    if (this.positionUpdateInterval) {
      clearInterval(this.positionUpdateInterval);
      this.positionUpdateInterval = null;
    }
  }

  // 상태 변경 알림
  notifyStateChange() {
    this.listeners.forEach(listener => {
      try {
        listener(this.getState());
      } catch (error) {
        console.error('State listener error:', error);
      }
    });
  }

  // 현재 상태 반환
  getState() {
    return {
      player: this.player,
      deviceId: this.deviceId,
      accessToken: this.accessToken,
      isReady: this.isReady,
      currentTrack: this.currentTrack,
      isPlaying: this.isPlaying,
      position: this.position,
      duration: this.duration,
      isPaused: this.isPaused,
      canControl: this.canControl,
      volume: this.volume
    };
  }

  // Spotify 액세스 토큰 가져오기
  async getSpotifyAccessToken() {
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
        this.accessToken = data.accessToken;
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
  }

  // Spotify SDK 동적 로드
  loadSpotifySDK() {
    return new Promise((resolve, reject) => {
      if (this.sdkLoaded && window.Spotify && window.Spotify.Player) {
        console.log('Spotify SDK 이미 로드됨');
        resolve(window.Spotify);
        return;
      }

      if (this.sdkLoading) {
        // 이미 로딩 중이면 대기
        const checkLoaded = () => {
          if (this.sdkLoaded && window.Spotify && window.Spotify.Player) {
            resolve(window.Spotify);
          } else {
            setTimeout(checkLoaded, 100);
          }
        };
        checkLoaded();
        return;
      }

      this.sdkLoading = true;
      console.log('Spotify SDK 로딩 시작...');
      
      const existingScript = document.querySelector('script[src="https://sdk.scdn.co/spotify-player.js"]');
      if (existingScript) {
        if (window.Spotify && window.Spotify.Player) {
          this.sdkLoaded = true;
          this.sdkLoading = false;
          resolve(window.Spotify);
        } else {
          window.onSpotifyWebPlaybackSDKReady = () => {
            console.log('Spotify SDK 로드 완료');
            this.sdkLoaded = true;
            this.sdkLoading = false;
            resolve(window.Spotify);
          };
        }
        return;
      }

      const script = document.createElement('script');
      script.src = 'https://sdk.scdn.co/spotify-player.js';
      script.async = true;
      
      window.onSpotifyWebPlaybackSDKReady = () => {
        console.log('Spotify SDK 로드 완료');
        this.sdkLoaded = true;
        this.sdkLoading = false;
        resolve(window.Spotify);
      };
      
      script.onload = () => {
        console.log('Spotify SDK 스크립트 로드됨');
      };
      
      script.onerror = () => {
        console.error('Spotify SDK 스크립트 로드 실패');
        this.sdkLoading = false;
        reject(new Error('Spotify SDK 로드 실패'));
      };
      
      document.head.appendChild(script);
      
      setTimeout(() => {
        if (!this.sdkLoaded) {
          console.error('Spotify SDK 로드 타임아웃');
          this.sdkLoading = false;
          reject(new Error('Spotify SDK 로드 타임아웃'));
        }
      }, 15000);
    });
  }

  // Spotify Player 초기화
  async initializePlayer() {
    if (this.player && this.isReady) {
      console.log('Spotify Player 이미 초기화됨');
      return this.player;
    }

    try {
      const token = await this.getSpotifyAccessToken();
      if (!token) {
        console.log('Spotify 액세스 토큰이 없습니다. 플레이어를 초기화하지 않습니다.');
        return null;
      }

      await this.loadSpotifySDK();

      console.log('Spotify Web Playback SDK 초기화 시작...');
      
      const spotifyPlayer = new window.Spotify.Player({
        name: 'Moody Web Player',
        getOAuthToken: cb => { cb(token); },
        volume: this.volume
      });

      this.player = spotifyPlayer;

      // 플레이어 이벤트 리스너 설정
      spotifyPlayer.addListener('ready', ({ device_id }) => {
        console.log('Ready with Device ID', device_id);
        this.deviceId = device_id;
        this.isReady = true;
        this.transferPlaybackToThisDevice(device_id);
        this.notifyStateChange();
      });

      spotifyPlayer.addListener('not_ready', ({ device_id }) => {
        console.log('Device ID has gone offline', device_id);
        this.isReady = false;
        this.notifyStateChange();
      });

      spotifyPlayer.addListener('player_state_changed', (state) => {
        console.log('Player state changed:', state);
        
        if (!state) {
          this.canControl = false;
          this.currentTrack = null;
          this.isPlaying = false;
          this.isPaused = true;
          this.stopPositionUpdate();
          this.notifyStateChange();
          return;
        }

        this.currentTrack = state.track_window.current_track;
        this.isPlaying = !state.paused;
        this.isPaused = state.paused;
        this.position = state.position;
        this.duration = state.duration;
        this.canControl = true;
        this.lastPositionUpdate = Date.now();
        
        // 재생 중이면 position 업데이트 시작, 일시정지면 중지
        if (this.isPlaying) {
          this.startPositionUpdate();
        } else {
          this.stopPositionUpdate();
        }
        
        this.notifyStateChange();
      });

      // 오류 리스너들
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
      const success = await spotifyPlayer.connect();
      if (success) {
        console.log('Spotify Player 연결 성공!');
        return spotifyPlayer;
      } else {
        console.error('Spotify Player 연결 실패');
        return null;
      }

    } catch (error) {
      console.error('Spotify Player 초기화 중 전체 오류:', error);
      return null;
    }
  }

  // 이 디바이스로 재생 전환
  async transferPlaybackToThisDevice(deviceId) {
    if (!this.accessToken) return;
    
    try {
      console.log('재생을 이 디바이스로 전환 중...', deviceId);
      
      const response = await fetch('https://api.spotify.com/v1/me/player', {
        method: 'PUT',
        headers: {
          'Authorization': `Bearer ${this.accessToken}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          device_ids: [deviceId],
          play: false
        })
      });

      if (response.status === 204) {
        console.log('재생 디바이스 전환 완료');
        setTimeout(() => {
          this.checkCurrentPlaybackState();
        }, 1000);
      } else {
        console.log('재생 디바이스 전환 응답:', response.status);
      }
    } catch (error) {
      console.error('재생 디바이스 전환 오류:', error);
    }
  }

  // 현재 재생 상태 확인
  async checkCurrentPlaybackState() {
    if (!this.accessToken) return;
    
    try {
      const response = await fetch('https://api.spotify.com/v1/me/player', {
        headers: {
          'Authorization': `Bearer ${this.accessToken}`,
        },
      });

      if (response.ok) {
        const data = await response.json();
        console.log('현재 재생 상태:', data);
        
        if (data && data.item) {
          this.currentTrack = data.item;
          this.isPlaying = data.is_playing;
          this.isPaused = !data.is_playing;
          this.canControl = true;
          this.position = data.progress_ms || 0;
          this.duration = data.item.duration_ms || 0;
          this.lastPositionUpdate = Date.now();
          
          // 재생 중이면 position 업데이트 시작, 일시정지면 중지
          if (this.isPlaying) {
            this.startPositionUpdate();
          } else {
            this.stopPositionUpdate();
          }
          
          this.notifyStateChange();
        }
      } else if (response.status === 204) {
        console.log('현재 재생 중인 곡이 없습니다');
      }
    } catch (error) {
      console.error('재생 상태 확인 오류:', error);
    }
  }

  // 플레이어 연결 해제
  disconnect() {
    this.stopPositionUpdate();
    if (this.player) {
      this.player.disconnect();
      this.player = null;
      this.isReady = false;
      this.deviceId = null;
      this.notifyStateChange();
    }
  }

  // 트랙 목록을 재생목록에 추가 (기존 로직) -> 비공개 헬퍼로 변경
  async _addTracksToEndOfQueue(trackUris) {
    if (!this.accessToken || !this.deviceId || !trackUris || trackUris.length === 0) {
      return false;
    }

    try {
      console.log(`${trackUris.length}개의 트랙을 큐의 끝에 추가합니다...`);
      for (const uri of trackUris) {
        const response = await fetch(`https://api.spotify.com/v1/me/player/queue?uri=${encodeURIComponent(uri)}&device_id=${this.deviceId}`, {
          method: 'POST',
          headers: { 'Authorization': `Bearer ${this.accessToken}` },
        });
        if (!response.ok) {
          console.error(`트랙 추가 실패 (URI: ${uri}):`, response.status);
          return false;
        }
      }
      console.log('모든 트랙이 큐에 성공적으로 추가되었습니다.');
      setTimeout(() => this.checkCurrentPlaybackState(), 1000);
      return true;

    } catch (error) {
      console.error('큐 추가 중 오류 발생:', error);
      return false;
    }
  }

  // 추천 트랙 목록으로 즉시 재생 시작
  async _playTracksImmediately(trackUris) {
    if (!this.accessToken || !this.deviceId || !trackUris || trackUris.length === 0) {
      return false;
    }
    try {
      console.log('추천 트랙으로 즉시 재생을 시작합니다...');
      const response = await fetch(`https://api.spotify.com/v1/me/player/play?device_id=${this.deviceId}`, {
        method: 'PUT',
        headers: {
          'Authorization': `Bearer ${this.accessToken}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ uris: trackUris }),
      });

      if (response.ok) {
        console.log('추천 트랙 재생 시작 성공.');
        setTimeout(() => this.checkCurrentPlaybackState(), 1000);
        return true;
      } else {
        console.error('즉시 재생 실패:', response.status, await response.text());
        return false;
      }
    } catch (error) {
      console.error('즉시 재생 중 오류:', error);
      return false;
    }
  }

  // 컨텍스트에 따라 큐에 추가하거나 즉시 재생
  async addTracksToQueue(trackUris) {
    if (!this.accessToken || !this.deviceId) {
      return { success: false, playedImmediately: false };
    }

    try {
      const response = await fetch('https://api.spotify.com/v1/me/player', {
        headers: { 'Authorization': `Bearer ${this.accessToken}` },
      });

      let hasUserContext = false;
      if (response.ok && response.status !== 204) {
        const data = await response.json();
        if (data && data.context && data.context.uri) {
          hasUserContext = true;
        }
      }

      if (hasUserContext) {
        console.log('사용자 지정 컨텍스트 감지. 큐의 끝에 추가합니다.');
        const success = await this._addTracksToEndOfQueue(trackUris);
        return { success, playedImmediately: false };
      } else {
        console.log('자연 재생 또는 비활성 상태 감지. 즉시 재생합니다.');
        const success = await this._playTracksImmediately(trackUris);
        return { success, playedImmediately: true };
      }

    } catch (error) {
      console.error('플레이어 상태 확인 중 오류, 즉시 재생으로 대체:', error);
      const success = await this._playTracksImmediately(trackUris);
      return { success, playedImmediately: true };
    }
  }
}

// 싱글톤 인스턴스
const spotifyPlayerService = new SpotifyPlayerService();

export default spotifyPlayerService; 