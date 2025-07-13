import { useState } from "react";
import { useNavigate } from "react-router-dom";
import "./Onboarding.css";

function Onboarding() {
  const [step, setStep] = useState(1);
  const [userData, setUserData] = useState({
    nickname: "",
    musicPreferences: []
  });
  const navigate = useNavigate();

  const musicGenres = [
    "팝", "락", "재즈", "클래식", "힙합", "R&B", 
    "일렉트로닉", "컨트리", "레게", "블루스", "메탈", "포크"
  ];

  const handleNicknameSubmit = (e) => {
    e.preventDefault();
    if (userData.nickname.trim()) {
      setStep(2);
    }
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
        console.log('온보딩 완료 성공, dashboard로 이동');
        // 로컬 스토리지에 저장
        localStorage.setItem('userData', JSON.stringify(userData));
        localStorage.setItem('onboardingCompleted', 'true');
        // Dashboard로 이동
        navigate('/dashboard');
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
          <img 
            src="/moody_logo.png" 
            alt="Moody Logo" 
            className="onboarding-logo"
          />
          <h1>Moody에 오신 것을 환영합니다</h1>
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
            <h2>음악 취향을 알려주세요</h2>
            <p>좋아하는 음악 장르를 선택해주세요. (복수 선택 가능)</p>
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
                onClick={() => setStep(1)}
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