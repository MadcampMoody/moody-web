import { useState } from "react";
import { useNavigate } from "react-router-dom";
import "./Onboarding.css";

function Onboarding() {
  const [step, setStep] = useState(1);
  const [userData, setUserData] = useState({
    nickname: "",
    musicRegion: [],
    musicPreferences: []
  });
  const navigate = useNavigate();

  const musicGenres = [
    "팝", "락", "힙합", "R&B", "재즈", "K-POP", 
    "EDM", "컨트리", "댄스", "인디"
  ];

  const handleNicknameSubmit = (e) => {
    e.preventDefault();
    if (userData.nickname.trim()) {
      setStep(2);
    }
  };

  // musicRegion을 배열로 관리, toggle 방식
  const handleMusicRegionToggle = (region) => {
    setUserData(prev => {
      let newRegions = prev.musicRegion.includes(region)
        ? prev.musicRegion.filter(r => r !== region)
        : [...prev.musicRegion, region];
      return { ...prev, musicRegion: newRegions };
    });
  };

  const handleMusicRegionNext = () => {
    setStep(3);
  };

  const handleMusicPreferenceToggle = (genre) => {
    setUserData(prev => ({
      ...prev,
      musicPreferences: prev.musicPreferences.includes(genre)
        ? prev.musicPreferences.filter(g => g !== genre)
        : [...prev.musicPreferences, genre]
    }));
  };

  const handleComplete = async () => {
    console.log('handleComplete 호출됨');
    try {
      console.log('백엔드로 데이터 전송 시작:', userData);
      // 백엔드로 온보딩 데이터 전송
      const response = await fetch('/api/auth/onboarding-complete', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        credentials: 'include',
        body: JSON.stringify(userData)
      });

      console.log('백엔드 응답:', response.status);
      if (response.ok) {
        const result = await response.json();
        console.log('온보딩 완료 성공:', result);
        console.log('업데이트된 사용자 정보:', result.user);
        // 잠시 대기 후 Dashboard로 이동 (데이터베이스 업데이트 시간 확보)
        setTimeout(() => {
          navigate('/dashboard');
        }, 1000);
      } else {
        console.error('온보딩 완료 실패');
        // 에러가 있어도 dashboard로 이동
        navigate('/dashboard');
      }
    } catch (error) {
      console.error('온보딩 완료 중 오류:', error);
      // 에러가 있어도 dashboard로 이동
      navigate('/dashboard');
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
                value={userData.nickname}
                onChange={(e) => setUserData(prev => ({ ...prev, nickname: e.target.value }))}
                className="nickname-input"
                maxLength={20}
                required
              />
              <button type="submit" className="next-btn" disabled={!userData.nickname.trim()}>
                다음
              </button>
            </form>
          </div>
        )}

        {step === 2 && (
          <div className="onboarding-step">
            <div style={{display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px'}}>
              <h2 style={{margin: 0}}>어떤 음악을 선호하시나요?</h2>
              <span className="multi-select-hint">(복수 선택 가능)</span>
            </div>
            <div className="music-region-selection">
              <button
                className={`region-btn ${userData.musicRegion.includes('domestic') ? 'selected' : ''}`}
                onClick={() => handleMusicRegionToggle('domestic')}
                type="button"
              >
                <div className="region-icon">🇰🇷</div>
                <div className="region-text">
                  <h3>국내음악</h3>
                  <p>K-POP, 국내 가수 음악</p>
                </div>
              </button>
              <button
                className={`region-btn ${userData.musicRegion.includes('international') ? 'selected' : ''}`}
                onClick={() => handleMusicRegionToggle('international')}
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
                disabled={userData.musicRegion.length === 0}
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
                  className={`genre-btn ${userData.musicPreferences.includes(genre) ? 'selected' : ''}`}
                  onClick={() => handleMusicPreferenceToggle(genre)}
                >
                  {genre}
                </button>
              ))}
            </div>
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
                disabled={userData.musicPreferences.length === 0}
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