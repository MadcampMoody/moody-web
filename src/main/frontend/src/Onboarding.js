import { useState } from "react";
import { useNavigate } from "react-router-dom";
import "./Onboarding.css";
import axios from './axios-instance'; // 수정된 axios 인스턴스 import

function Onboarding() {
  const [step, setStep] = useState(1);
  const [userData, setUserData] = useState({
    name: "", // nickname -> name
    musicRegion: "", // 배열 -> 단일 값
    musicGenres: [] // musicPreferences -> musicGenres
  });
  const [error, setError] = useState(""); // 에러 상태 추가
  const navigate = useNavigate();

  const musicGenres = [
    "팝", "락", "힙합", "R&B", "재즈", "K-POP", 
    "EDM", "컨트리", "댄스", "인디"
  ];

  const handleNicknameSubmit = (e) => {
    e.preventDefault();
    if (userData.name.trim()) {
      setStep(2);
    }
  };

  // musicRegion을 단일 값으로 관리
  const handleMusicRegionSelect = (region) => {
    setUserData(prev => ({ ...prev, musicRegion: region }));
  };

  const handleMusicRegionNext = () => {
    if (userData.musicRegion) {
        setStep(3);
    }
  };

  const handleMusicPreferenceToggle = (genre) => {
    setUserData(prev => ({
      ...prev,
      musicGenres: prev.musicGenres.includes(genre)
        ? prev.musicGenres.filter(g => g !== genre)
        : [...prev.musicGenres, genre]
    }));
  };

  const handleComplete = async () => {
    setError(""); // 에러 초기화
    try {
      // musicRegion 값을 domestic, international 소문자로 전송
      const dataToSend = {
        name: userData.name,
        musicRegion: userData.musicRegion, // 'domestic' 또는 'international'
        musicGenres: userData.musicGenres
      };
      
      // 이제 baseURL과 withCredentials가 자동으로 적용됩니다.
      await axios.post('/api/auth/onboarding', dataToSend, {
        headers: { 'Content-Type': 'application/json' },
      });

      navigate('/dashboard');

    } catch (err) {
      setError("온보딩 정보를 저장하는 데 실패했습니다. 다시 시도해주세요.");
      console.error('온보딩 완료 중 오류:', err);
    }
  };

  return (
    <div className="onboarding-container">
      <div className="onboarding-card">
        <div className="onboarding-header">
          <div className="onboarding-logo-wrapper">
            <img 
              src="/moody_logo.png" 
              alt="Moody Logo" 
              className="onboarding-logo"
            />
          </div>
          <div className="onboarding-title">moody</div>
        </div>

        {step === 1 && (
          <div className="onboarding-step">
            <h2>Moody에서 사용하실 이름을 알려주세요!</h2>
            <form onSubmit={handleNicknameSubmit} className="nickname-form">
              <input
                type="text"
                placeholder="닉네임을 입력하세요"
                value={userData.name}
                onChange={(e) => setUserData(prev => ({ ...prev, name: e.target.value }))}
                className="nickname-input"
                maxLength={20}
                required
              />
              <button type="submit" className="next-btn" disabled={!userData.name.trim()}>
                다음
              </button>
            </form>
          </div>
        )}

        {step === 2 && (
          <div className="onboarding-step">
            <div style={{display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px'}}>
              <h2 style={{margin: 0}}>어떤 음악을 선호하시나요?</h2>
            </div>
            <div className="music-region-selection">
              <button
                className={`region-btn ${userData.musicRegion === 'domestic' ? 'selected' : ''}`}
                onClick={() => handleMusicRegionSelect('domestic')}
                type="button"
              >
                <div className="region-icon">🇰🇷</div>
                <div className="region-text">
                  <h3>국내음악</h3>
                  <p>K-POP, 국내 가수 음악</p>
                </div>
              </button>
              <button
                className={`region-btn ${userData.musicRegion === 'international' ? 'selected' : ''}`}
                onClick={() => handleMusicRegionSelect('international')}
                type="button"
              >
                <div className="region-icon">🌍</div>
                <div className="region-text">
                  <h3>해외음악</h3>
                  <p>해외 가수, 글로벌 음악</p>
                </div>
              </button>
            </div>
            <div className="onboarding-actions">
              <button 
                className="back-btn" 
                onClick={() => setStep(1)}
              >
                이전
              </button>
              <button 
                className="next-btn" 
                onClick={handleMusicRegionNext}
                disabled={!userData.musicRegion}
              >
                다음
              </button>
            </div>
          </div>
        )}

        {step === 3 && (
          <div className="onboarding-step">
            <div style={{display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px'}}>
              <h2 style={{margin: 0}}>음악 취향을 알려주세요</h2>
              <span className="multi-select-hint">(복수 선택 가능)</span>
            </div>
            <div className="music-preferences">
              {musicGenres.map((genre) => (
                <button
                  key={genre}
                  className={`genre-btn ${userData.musicGenres.includes(genre) ? 'selected' : ''}`}
                  onClick={() => handleMusicPreferenceToggle(genre)}
                >
                  {genre}
                </button>
              ))}
            </div>
            {error && <p className="onboarding-error">{error}</p>}
            <div className="onboarding-actions">
              <button 
                className="back-btn" 
                onClick={() => setStep(2)}
              >
                이전
              </button>
              <button 
                className="complete-btn" 
                onClick={handleComplete}
                disabled={userData.musicGenres.length === 0}
              >
                완료
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

export default Onboarding; 