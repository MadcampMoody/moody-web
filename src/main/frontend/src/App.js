import { useEffect, useState } from "react";
import { BrowserRouter as Router, Routes, Route, Navigate } from "react-router-dom";
import "./App.css";
import Onboarding from "./Onboarding";
import Dashboard from "./Dashboard";
import MusicRecommendPage from "./components/MusicRecommendPage";
import DiaryEditorWrapper from "./DiaryEditorWrapper";


function App() {
  const [error, setError] = useState('');

  useEffect(() => {
    // URL 파라미터에서 에러 확인
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.get('error') === 'true') {
      setError('카카오 로그인에 실패했습니다. 다시 시도해주세요.');
    }
  }, []);

  const handleKakaoLogin = () => {
    // 에러 메시지 초기화
    setError('');
    // 카카오 OAuth2 로그인 URL로 리다이렉트
    window.location.href = 'http://127.0.0.1:8080/oauth2/authorization/kakao';
  };

  // 메인 페이지 (로그인 페이지)
  const LoginPage = () => {
    return (
      <div className="App">
        <div className="login-container">
          <div className="logo-section">
            <img 
              src="/moody_logo.png" 
              alt="Moody Logo" 
              className="moody-logo"
            />
          </div>
          <div className="login-section">
            {error && (
              <div className="error-message">
                {error}
              </div>
            )}
            <button 
              className="kakao-login-btn"
              onClick={handleKakaoLogin}
            >
              카카오 로그인하기
            </button>
          </div>
        </div>
      </div>
    );
  };

  return (
    <Router>
      <Routes>
        <Route path="/" element={<LoginPage />} />
        <Route path="/onboarding" element={<Onboarding />} />
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/music_recommend" element={<MusicRecommendPage />} />
        <Route path="/diary/:date" element={<DiaryEditorWrapper />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Router>
  );
}

export default App;