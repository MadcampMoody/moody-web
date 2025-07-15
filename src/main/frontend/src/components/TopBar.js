import React from 'react';
import { useNavigate } from 'react-router-dom';
import './TopBar.css';

const TopBar = () => {
  const navigate = useNavigate();

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
      </div>
    </div>
  );
};

export default TopBar; 