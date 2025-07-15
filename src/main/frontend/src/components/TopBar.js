import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import './TopBar.css';
import SpotifyPlayer from './SpotifyPlayer';

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
        {/* Spotify 로그인이 되어있을 때만 플레이어 표시 */}
        {!loading && isSpotifyLoggedIn && <SpotifyPlayer />}
        
        {/* Spotify 로그인이 안되어있을 때만 로그인 버튼 표시 */}
        {!loading && !isSpotifyLoggedIn && (
          <button className="spotify-login-btn" onClick={handleSpotifyLogin}>
            🎵 Spotify 로그인
          </button>
        )}
        
        {/* 로딩 중일 때 표시 */}
        {loading && (
          <div className="spotify-loading">
            로딩 중...
          </div>
        )}
        
        <button className="profile-btn" onClick={handleProfile}>
          내 프로필
        </button>
      </div>
    </div>
  );
};

export default TopBar; 