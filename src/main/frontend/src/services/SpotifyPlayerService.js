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
        console.log('토큰 존재 여부:', data.accessToken ? '있음' : '없음');
        if (data.accessToken) {
          console.log('토큰 길이:', data.accessToken.length);
          console.log('토큰 시작 부분:', data.accessToken.substring(0, 20) + '...');
        }
        this.accessToken = data.accessToken;
        return data.accessToken;
      } else {
        const errorData = await response.json().catch(() => ({}));
        console.error('Spotify 액세스 토큰 가져오기 실패:', response.status, errorData);
        console.error('오류 세부 정보:', errorData);
        return null;
      }
    } catch (error) {
      console.error('Spotify 액세스 토큰 요청 오류:', error);
      console.error('오류 타입:', error.name);
      console.error('오류 메시지:', error.message);
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
      // 먼저 Spotify 연동 상태 확인
      console.log('Spotify 연동 상태 확인 중...');
      const statusResponse = await fetch('http://127.0.0.1:8080/api/spotify/status', {
        method: 'GET',
        credentials: 'include',
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json',
        },
      });
      
      if (statusResponse.ok) {
        const statusData = await statusResponse.json();
        console.log('Spotify 연동 상태:', statusData);
        if (!statusData.spotifyLinked) {
          console.log('Spotify가 연동되지 않았습니다. Spotify 로그인이 필요합니다.');
          return null;
        }
      } else {
        console.log('Spotify 상태 확인 실패:', statusResponse.status);
        // 상태 확인 실패해도 토큰 요청은 시도해보기 (자동 갱신 가능)
      }

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

      let hasNaturalPlayback = false;
      let contextInfo = null;
      let shuffleState = false;
      
      if (response.ok && response.status !== 204) {
        const data = await response.json();
        if (data && data.is_playing !== undefined) {
          // 셔플 상태 확인
          shuffleState = data.shuffle_state || false;
          
          // 자연스러운 재생 상태 판단 기준:
          // 1. 현재 재생 중이거나 일시정지 상태
          // 2. 컨텍스트가 있음 (플레이리스트, 앨범, 아티스트 등)
          // 3. 반복/셔플 모드 여부와 상관없이 사용자가 의도적으로 선택한 컨텍스트
          if (data.context && data.context.uri) {
            hasNaturalPlayback = true;
            contextInfo = {
              uri: data.context.uri,
              type: data.context.type,
              href: data.context.href
            };
            console.log('자연스러운 재생 컨텍스트 감지:', contextInfo);
            console.log('셔플 모드 상태:', shuffleState ? '활성화' : '비활성화');
          } else if (data.item && !data.context) {
            // 컨텍스트가 없지만 현재 재생 중인 곡이 있는 경우
            // (단일 곡 재생, 이전에 추가된 큐 등)
            console.log('단일 곡 또는 큐 재생 상태 감지');
            hasNaturalPlayback = false;
          }
        }
      }

      if (hasNaturalPlayback) {
        console.log('자연스러운 재생 상태입니다. 추천 음악을 큐에 추가하여 현재 컨텍스트 이후 재생되도록 합니다.');
        console.log(`컨텍스트 정보: ${contextInfo.type} - ${contextInfo.uri}`);
        
        // 큐의 끝에 순차적으로 추가
        const success = await this._addTracksToQueueSequentially(trackUris);
        return { 
          success, 
          playedImmediately: false, 
          preservedContext: true, 
          shuffleEnabled: shuffleState 
        };
      } else {
        console.log('자연스러운 재생 상태가 아닙니다. 추천 음악으로 즉시 재생을 시작합니다.');
        const success = await this._playTracksImmediately(trackUris);
        return { 
          success, 
          playedImmediately: true, 
          preservedContext: false, 
          shuffleEnabled: shuffleState 
        };
      }

    } catch (error) {
      console.error('플레이어 상태 확인 중 오류가 발생했습니다. 추천 음악으로 즉시 재생합니다:', error);
      const success = await this._playTracksImmediately(trackUris);
      return { 
        success, 
        playedImmediately: true, 
        preservedContext: false, 
        shuffleEnabled: false 
      };
    }
  }

  // 트랙 목록을 큐에 순차적으로 추가 (개선된 버전)
  async _addTracksToQueueSequentially(trackUris) {
    if (!this.accessToken || !this.deviceId || !trackUris || trackUris.length === 0) {
      return false;
    }

    try {
      console.log(`${trackUris.length}개의 추천 음악을 큐에 순차적으로 추가합니다...`);
      
      // 각 트랙을 200ms 간격으로 순차 추가하여 순서 보장
      for (let i = 0; i < trackUris.length; i++) {
        const uri = trackUris[i];
        console.log(`트랙 ${i + 1}/${trackUris.length} 추가 중: ${uri}`);
        
        const response = await fetch(`https://api.spotify.com/v1/me/player/queue?uri=${encodeURIComponent(uri)}&device_id=${this.deviceId}`, {
          method: 'POST',
          headers: { 'Authorization': `Bearer ${this.accessToken}` },
        });
        
        if (!response.ok) {
          console.error(`트랙 추가 실패 (${i + 1}/${trackUris.length}, URI: ${uri}):`, response.status);
          const errorText = await response.text();
          console.error('오류 세부 정보:', errorText);
          return false;
        }
        
        // 순서 보장을 위한 짧은 지연
        if (i < trackUris.length - 1) {
          await new Promise(resolve => setTimeout(resolve, 200));
        }
      }
      
      console.log('모든 추천 음악이 큐에 성공적으로 추가되었습니다.');
      console.log('추천 음악 재생 완료 후 원래 자연스러운 재생이 이어집니다.');
      
      // 상태 업데이트
      setTimeout(() => this.checkCurrentPlaybackState(), 1000);
      return true;

    } catch (error) {
      console.error('큐에 순차 추가 중 오류 발생:', error);
      return false;
    }
  }

  // 현재 큐 정보 가져오기
  async getCurrentQueue() {
    if (!this.accessToken || !this.deviceId) {
      return null;
    }

    try {
      const response = await fetch(`https://api.spotify.com/v1/me/player/queue?device_id=${this.deviceId}`, {
        headers: { 'Authorization': `Bearer ${this.accessToken}` },
      });

      if (response.ok) {
        const data = await response.json();
        console.log('현재 큐 정보:', data);
        return data;
      } else {
        console.error('큐 정보 가져오기 실패:', response.status);
        return null;
      }
    } catch (error) {
      console.error('큐 정보 가져오기 오류:', error);
      return null;
    }
  }
}

// 싱글톤 인스턴스
const spotifyPlayerService = new SpotifyPlayerService();

export default spotifyPlayerService; 