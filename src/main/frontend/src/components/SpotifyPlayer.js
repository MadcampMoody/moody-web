import React, { useEffect, useRef, useContext } from 'react';
import './SpotifyPlayer.css';
import { SpotifyContext } from '../contexts/SpotifyContext';

const SpotifyPlayer = () => {
  const {
    isSpotifyLoggedIn,
    accessToken,
    loading,
    player,
    currentTrack,
    isPaused,
    isActive,
    position,
    togglePlay,
    nextTrack,
    previousTrack,
    seek,
  } = useContext(SpotifyContext);

  const trackNameRef = useRef(null);
  const artistNameRef = useRef(null);

  // 시간 포맷팅
  const formatTime = (ms) => {
    if (!ms) return "0:00";
    const minutes = Math.floor(ms / 60000);
    const seconds = Math.floor((ms % 60000) / 1000);
    return `${minutes}:${seconds.toString().padStart(2, '0')}`;
  };

  // 진행바 클릭으로 재생 위치 이동
  const handleProgressBarClick = async (event) => {
    if (!currentTrack) return;
    try {
      const progressBar = event.currentTarget;
      const rect = progressBar.getBoundingClientRect();
      const clickX = event.clientX - rect.left;
      const progressBarWidth = rect.width;
      const clickPercentage = clickX / progressBarWidth;
      const seekPosition = Math.floor(currentTrack.duration_ms * clickPercentage);
      await seek(seekPosition);
    } catch (error) {
      console.error('재생 위치 이동 오류:', error);
    }
  };

  const duration = currentTrack ? currentTrack.duration_ms : 0;
  const canControl = !!currentTrack;
  const isPlaying = !isPaused;

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

  if (!accessToken) {
    return (
      <div className="spotify-player loading">
        <div className="loading-text">Spotify 인증 확인 중...</div>
      </div>
    );
  }
  
  if (!player || !isActive) {
    return (
      <div className="spotify-player">
         <div className="now-playing">
           <div className="track-info">
             <div className="no-track-placeholder">
              <svg width="48" height="48" viewBox="0 0 24 24" fill="none">
                 <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z" fill="rgba(255,255,255,0.5)"/>
               </svg>
             </div>
             <div className="track-details">
               <div className="track-name">재생 중인 곡이 없습니다</div>
               <div className="artist-name">Spotify에서 음악을 재생하세요</div>
             </div>
           </div>
         </div>
         <div className="player-controls">
            {/* Render disabled-looking controls */}
         </div>
         <div className="progress-section">
            {/* Render disabled-looking progress */}
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
          onClick={togglePlay}
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