import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import './TopBar.css';

const TopBar = () => {
  const navigate = useNavigate();
  const [isSpotifyLoggedIn, setIsSpotifyLoggedIn] = useState(false);
  const [loading, setLoading] = useState(true);

  // Spotify 로그인 상태 확인
  const checkSpotifyLoginStatus = async () => {
    try {
      console.log('=== Spotify 상태 확인 시작 ===');
      const response = await fetch('http://127.0.0.1:8080/api/auth/spotify-status', {
        credentials: 'include',
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json',
        },
      });
      
      console.log('응답 상태:', response.status);
      
      if (response.ok) {
        const data = await response.json();
        console.log('서버 응답 데이터:', data);
        setIsSpotifyLoggedIn(data.spotifyLoggedIn);
        console.log('Spotify 로그인 상태 설정:', data.spotifyLoggedIn);
      } else {
        console.log('응답이 실패했습니다. 상태:', response.status);
        setIsSpotifyLoggedIn(false);
      }
    } catch (error) {
      console.error('Spotify 로그인 상태 확인 오류:', error);
      setIsSpotifyLoggedIn(false);
    } finally {
      setLoading(false);
      console.log('=== Spotify 상태 확인 완료 ===');
    }
  };

  useEffect(() => {
    checkSpotifyLoginStatus();
  }, []);

  // 페이지에 포커스가 돌아올 때 상태 재확인 (로그인 후 돌아왔을 때)
  useEffect(() => {
    const handleFocus = () => {
      console.log('페이지 포커스 복귀 - Spotify 로그인 상태 재확인');
      checkSpotifyLoginStatus();
    };

    window.addEventListener('focus', handleFocus);
    return () => window.removeEventListener('focus', handleFocus);
  }, []);

  const handleLogoClick = () => {
    navigate('/dashboard');
  };

  const handlePrevious = () => {
    console.log('이전 곡');
    // TODO: Spotify 이전 곡 기능 구현
  };

  const handlePlayPause = () => {
    console.log('재생/일시정지');
    // TODO: Spotify 재생/일시정지 기능 구현
  };

  const handleNext = () => {
    console.log('다음 곡');
    // TODO: Spotify 다음 곡 기능 구현
  };

  const handleProfile = () => {
    console.log('내 프로필');
    // TODO: 프로필 페이지로 이동
  };

  const handleSpotifyLogin = () => {
    // 스포티파이 OAuth2 로그인 URL로 리다이렉트
    window.location.href = 'http://127.0.0.1:8080/oauth2/authorization/spotify';
  };

  return (
    <div className="top-bar">
      <div className="top-bar-left">
        <button className="logo-button" onClick={handleLogoClick}>
          무디 요요
        </button>
      </div>
      
      <div className="top-bar-right">
        <div className="player-controls">
          <button className="control-btn previous-btn" onClick={handlePrevious}>
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
              <path d="M6 6h2v12H6zm3.5 6l8.5 6V6z" fill="currentColor"/>
            </svg>
          </button>
          
          <button className="control-btn play-pause-btn" onClick={handlePlayPause}>
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
              <path d="M8 5v14l11-7z" fill="currentColor"/>
            </svg>
          </button>
          
          <button className="control-btn next-btn" onClick={handleNext}>
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
              <path d="M6 18l8.5-6L6 6v12zM16 6v12h2V6h-2z" fill="currentColor"/>
            </svg>
          </button>
        </div>
        
        {!loading && (
          <button 
            className={isSpotifyLoggedIn ? "spotify-success-btn" : "spotify-login-btn"} 
            onClick={isSpotifyLoggedIn ? undefined : handleSpotifyLogin}
            disabled={isSpotifyLoggedIn}
          >
            {isSpotifyLoggedIn ? "✅ Spotify 로그인 성공!" : "🎵 Spotify 로그인"}
          </button>
        )}
        
        <button className="profile-btn" onClick={handleProfile}>
          내 프로필
        </button>
      </div>
    </div>
  );
};

export default TopBar; 