import React, { createContext, useState, useEffect, useRef, useCallback } from 'react';

export const SpotifyContext = createContext();

export const SpotifyProvider = ({ children }) => {
  const [isSpotifyLoggedIn, setIsSpotifyLoggedIn] = useState(false);
  const [accessToken, setAccessToken] = useState(null);
  const [loading, setLoading] = useState(true);
  
  // Player Instance and State
  const [player, setPlayer] = useState(null);
  const playerRef = useRef(null);
  const [deviceId, setDeviceId] = useState(null);
  const [currentTrack, setCurrentTrack] = useState(null);
  const [isPaused, setIsPaused] = useState(true);
  const [isActive, setIsActive] = useState(false);
  const [position, setPosition] = useState(0);

  const checkSpotifyLoginStatus = useCallback(async () => {
    try {
      const response = await fetch('http://127.0.0.1:8080/api/auth/spotify-status', {
        credentials: 'include',
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json',
        },
      });
      
      if (response.ok) {
        const data = await response.json();
        setIsSpotifyLoggedIn(data.spotifyLoggedIn);
        if (data.spotifyLoggedIn) {
          // 로그인 상태이면 토큰도 가져옴
          await getSpotifyAccessToken();
        }
      } else {
        setIsSpotifyLoggedIn(false);
        setAccessToken(null);
      }
    } catch (error) {
      console.error('Spotify 로그인 상태 확인 오류:', error);
      setIsSpotifyLoggedIn(false);
      setAccessToken(null);
    } finally {
      setLoading(false);
    }
  }, []);

  const getSpotifyAccessToken = useCallback(async () => {
    try {
      const response = await fetch('http://127.0.0.1:8080/api/spotify/access-token', {
        method: 'GET',
        credentials: 'include',
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json',
        },
      });
      
      if (response.ok) {
        const data = await response.json();
        setAccessToken(data.accessToken);
      } else {
        setAccessToken(null);
      }
    } catch (error) {
      console.error('Spotify 액세스 토큰 요청 오류:', error);
      setAccessToken(null);
    }
  }, []);

  // Player Controls
  const togglePlay = useCallback(async () => {
    if (!player) return;
    await player.togglePlay();
  }, [player]);

  const nextTrack = useCallback(async () => {
    if (!player) return;
    await player.nextTrack();
  }, [player]);

  const previousTrack = useCallback(async () => {
    if (!player) return;
    await player.previousTrack();
  }, [player]);

  const seek = useCallback(async (ms) => {
    if (!player) return;
    await player.seek(ms);
  }, [player]);

  useEffect(() => {
    // This effect runs once to check login status on load
    checkSpotifyLoginStatus();

    const handleFocus = () => {
      checkSpotifyLoginStatus();
    };

    window.addEventListener('focus', handleFocus);
    return () => window.removeEventListener('focus', handleFocus);
  }, [checkSpotifyLoginStatus]);

  const loadSpotifySDK = () => {
    return new Promise((resolve, reject) => {
      if (window.Spotify) {
        return resolve();
      }
      const script = document.createElement('script');
      script.src = 'https://sdk.scdn.co/spotify-player.js';
      script.async = true;
      document.body.appendChild(script);
      window.onSpotifyWebPlaybackSDKReady = () => {
        resolve();
      };
      script.onerror = reject;
    });
  };

  useEffect(() => {
    if (!accessToken) return;

    const initializePlayer = async () => {
      await loadSpotifySDK();
      
      const spotifyPlayer = new window.Spotify.Player({
        name: 'Moody Web Player',
        getOAuthToken: cb => { cb(accessToken); },
        volume: 0.5
      });

      spotifyPlayer.addListener('ready', ({ device_id }) => {
        setDeviceId(device_id);
        // Transfer playback to this device
        fetch('https://api.spotify.com/v1/me/player', {
          method: 'PUT',
          headers: {
            'Authorization': `Bearer ${accessToken}`,
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({ device_ids: [device_id] }),
        });
      });

      spotifyPlayer.addListener('player_state_changed', (state) => {
        if (!state) {
            setIsActive(false);
            return;
        }
        setCurrentTrack(state.track_window.current_track);
        setIsPaused(state.paused);
        setPosition(state.position);
        setIsActive(true);
      });

      await spotifyPlayer.connect();
      setPlayer(spotifyPlayer);
    };

    initializePlayer();

  }, [accessToken]);

  useEffect(() => {
    let intervalId;

    if (player && !isPaused) {
      intervalId = setInterval(async () => {
        if (player) {
          const state = await player.getCurrentState();
          if (state) {
            setPosition(state.position);
          }
        }
      }, 500);
    }

    return () => {
      if (intervalId) {
        clearInterval(intervalId);
      }
    };
  }, [player, isPaused]);

  const value = {
    isSpotifyLoggedIn,
    accessToken,
    loading,
    player,
    currentTrack,
    isPaused,
    isActive,
    position,
    deviceId,
    togglePlay,
    nextTrack,
    previousTrack,
    seek,
  };

  return (
    <SpotifyContext.Provider value={value}>
      {children}
    </SpotifyContext.Provider>
  );
}; 