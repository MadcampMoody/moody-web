import React, { useState, useEffect, useContext } from 'react';
import { useNavigate } from 'react-router-dom';
import './TopBar.css';
import SpotifyPlayer from './SpotifyPlayer';
import { SpotifyPlayerContext } from '../App';
import spotifyPlayerService from '../services/SpotifyPlayerService';

const TopBar = () => {
  const navigate = useNavigate();
  const { isSpotifyLoggedIn, loading, checkSpotifyLoginStatus } = useContext(SpotifyPlayerContext);
  const [playerInitialized, setPlayerInitialized] = useState(false);

  // 전역 SpotifyPlayerService 초기화 (첫 번째 TopBar 인스턴스에서만)
  useEffect(() => {
    const initializeGlobalPlayer = async () => {
      if (isSpotifyLoggedIn && !playerInitialized && !spotifyPlayerService.player) {
        console.log('전역 Spotify Player 초기화 시작...');
        try {
          await spotifyPlayerService.initializePlayer();
          setPlayerInitialized(true);
        } catch (error) {
          console.log('Spotify Player 초기화 실패 (조용히 처리):', error.message);
          // 초기화 실패해도 페이지 전체에 영향 없도록 조용히 처리
        }
      }
    };

    initializeGlobalPlayer();
  }, [isSpotifyLoggedIn, playerInitialized]);

  // 컴포넌트 언마운트 시 정리 (마지막 TopBar 인스턴스에서만)
  useEffect(() => {
    return () => {
      // 페이지 이동 시에는 플레이어를 끊지 않음
      // 오직 애플리케이션 전체가 종료될 때만 정리
    };
  }, []);

  const handleLogoClick = () => {
    navigate('/dashboard');
  };

  const handleProfile = () => {
    console.log('내 프로필 버튼 클릭');
    navigate('/profile');
  };

  const handleSpotifyLogin = () => {
    console.log('Spotify 로그인 버튼 클릭');
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