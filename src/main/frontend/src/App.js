import { useEffect, useState, createContext, useContext } from "react";
import { BrowserRouter as Router, Routes, Route, Navigate, useLocation } from "react-router-dom";
import "./App.css";
import Onboarding from "./Onboarding";
import Dashboard from "./Dashboard";
import MusicRecommendPage from "./components/MusicRecommendPage";
import DiaryEditorWrapper from "./DiaryEditorWrapper";
import TopBar from "./components/TopBar";


// Spotify Player Context 생성
export const SpotifyPlayerContext = createContext();

// Spotify Player Provider 컴포넌트
const SpotifyPlayerProvider = ({ children }) => {
  const [isSpotifyLoggedIn, setIsSpotifyLoggedIn] = useState(false);
  const [loading, setLoading] = useState(true);

  // Spotify 로그인 상태 확인
  const checkSpotifyLoginStatus = async () => {
    try {
      console.log('=== 전역 Spotify 상태 확인 시작 ===');
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
        console.log('전역 Spotify 로그인 상태 설정:', data.spotifyLoggedIn);
      } else {
        console.log('응답이 실패했습니다. 상태:', response.status);
        setIsSpotifyLoggedIn(false);
      }
    } catch (error) {
      console.error('전역 Spotify 로그인 상태 확인 오류:', error);
      setIsSpotifyLoggedIn(false);
    } finally {
      setLoading(false);
      console.log('=== 전역 Spotify 상태 확인 완료 ===');
    }
  };

  useEffect(() => {
    checkSpotifyLoginStatus();
    
    // 페이지에 포커스가 돌아올 때 상태 재확인
    const handleFocus = () => {
      console.log('페이지 포커스 복귀 - 전역 Spotify 로그인 상태 재확인');
      checkSpotifyLoginStatus();
    };

    window.addEventListener('focus', handleFocus);
    return () => window.removeEventListener('focus', handleFocus);
  }, []);

  return (
    <SpotifyPlayerContext.Provider value={{ 
      isSpotifyLoggedIn, 
      setIsSpotifyLoggedIn, 
      loading, 
      checkSpotifyLoginStatus 
    }}>
      {children}
      
    </SpotifyPlayerContext.Provider>
  );
};

// App Layout 컴포넌트 (TopBar 표시 여부 결정)
const AppLayout = () => {
  const location = useLocation();
  const isLoginPage = location.pathname === '/';
  const isOnboardingPage = location.pathname === '/onboarding';
  const shouldHideTopBar = isLoginPage || isOnboardingPage;

  return (
    <>
      {!shouldHideTopBar && <TopBar />}
      <div className={shouldHideTopBar ? "" : "main-content"}>
        <Routes>
          <Route path="/" element={<LoginPage />} />
          <Route path="/onboarding" element={<Onboarding />} />
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/music_recommend" element={<MusicRecommendPage />} />
          <Route path="/diary/:date" element={<DiaryEditorWrapper />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </div>
    </>
  );
};

// 메인 페이지 (로그인 페이지) - LoginPage를 함수 밖으로 이동
const LoginPage = () => {
  const [showKakao, setShowKakao] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    // URL 파라미터에서 에러 확인
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.get('error') === 'true') {
      setError('카카오 로그인에 실패했습니다. 다시 시도해주세요.');
    }
    
    const timer = setTimeout(() => setShowKakao(true), 1000);
    return () => clearTimeout(timer);
  }, []);

  const handleKakaoLogin = () => {
    // 에러 메시지 초기화
    setError('');
    // 카카오 OAuth2 로그인 URL로 리다이렉트
    window.location.href = 'http://127.0.0.1:8080/oauth2/authorization/kakao';
  };

  return (
    <div className="App login-bg">
      <div className="login-card">
        <div className="logo-section">
          <img 
            src="/moody_logo.png" 
            alt="Moody Logo" 
            className="moody-logo"
          />
        </div>
        <h1 className="service-title">moody</h1>
        <p className="service-desc">오늘의 감정을 기록하고, 나를 더 잘 이해해보세요</p>
        <div className="login-section">
          {error && (
            <div className="error-message">
              {error}
            </div>
          )}
          <button 
            className={`kakao-login-btn fade-in-btn${showKakao ? ' visible' : ''}`}
            onClick={handleKakaoLogin}
            disabled={!showKakao}
          >
            카카오 로그인하기
          </button>
        </div>
      </div>
    </div>
  );
};

function App() {
  return (
    <SpotifyPlayerProvider>
      <Router>
        <AppLayout />
      </Router>
    </SpotifyPlayerProvider>
  );
}

export default App;