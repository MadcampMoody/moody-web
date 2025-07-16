import React, { useContext } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import './TopBar.css';
import SpotifyPlayer from './SpotifyPlayer';
import { SpotifyContext } from '../contexts/SpotifyContext';

const TopBar = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { isSpotifyLoggedIn, loading } = useContext(SpotifyContext);

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

  const handleLogout = async () => {
    try {
      await fetch('/logout', {
        method: 'POST', // Use POST for logout
        credentials: 'include',
      });
      // On successful logout, redirect to the home page
      navigate('/');
    } catch (error) {
      console.error('Logout failed:', error);
      // Handle error if needed
    }
  };

  return (
    <div className="top-bar">
      <div className="top-bar-left">
        <img
          src="/moody_logo.png"
          alt="Moody Logo"
          className="logo-image"
          onClick={handleLogoClick}
        />
      </div>
      
      <div className="top-bar-right">
        {/* Spotify 로그인이 되어있고, 루트 경로가 아닐 때만 플레이어 표시 */}
        {!loading && isSpotifyLoggedIn && location.pathname !== '/' && <SpotifyPlayer />}
        
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
        
        {isSpotifyLoggedIn && location.pathname !== '/' && (
          <button className="logout-btn" onClick={handleLogout}>
            로그아웃
          </button>
        )}
      </div>
    </div>
  );
};

export default TopBar; 