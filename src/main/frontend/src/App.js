import { useEffect, useState } from "react";
import { BrowserRouter as Router, Routes, Route, Navigate, useLocation } from "react-router-dom";
import "./App.css";
import Onboarding from "./Onboarding";
import Dashboard from "./Dashboard";
import MusicRecommendPage from "./components/MusicRecommendPage";
import DiaryEditorWrapper from "./DiaryEditorWrapper";
import TopBar from "./components/TopBar";

function App() {
  return (
    <Router>
      <AppRoutes />
    </Router>
  );
}

function AppRoutes() {
  const location = useLocation();
  const [showSpotify, setShowSpotify] = useState(false);
  useEffect(() => {
    const timer = setTimeout(() => setShowSpotify(true), 1000);
    return () => clearTimeout(timer);
  }, []);
  // 카카오 관련 변수/함수/에러 메시지 삭제
  // Spotify 로그인 버튼만 남김
  const handleSpotifyLogin = () => {
    window.location.href = 'http://127.0.0.1:8080/oauth2/authorization/spotify';
  };

  return (
    <div className="App">
      {/* /, /onboarding에서는 TopBar 숨김 */}
      {location.pathname !== '/' && location.pathname !== '/onboarding' && <TopBar />}
      <Routes>
        <Route path="/" element={
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
                <button 
                  className={`spotify-login-btn fade-in-btn${showSpotify ? ' visible' : ''}`}
                  onClick={handleSpotifyLogin}
                  disabled={!showSpotify}
                >
                  Spotify로 로그인하기
                </button>
              </div>
            </div>
          </div>
        } />
        <Route path="/onboarding" element={<Onboarding />} />
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/music_recommend" element={<MusicRecommendPage />} />
        <Route path="/diary/:date" element={<DiaryEditorWrapper />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </div>
  );
}

export default App;