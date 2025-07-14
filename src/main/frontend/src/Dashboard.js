import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import MoodTracker from "./MoodTracker";
import "./Dashboard.css";

function Dashboard() {
  const [userData, setUserData] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    // URL에서 토큰 확인
    const urlParams = new URLSearchParams(window.location.search);
    const token = urlParams.get('token');
    
    // 토큰이 있으면 제거하고 URL 정리
    if (token) {
      window.history.replaceState({}, document.title, '/dashboard');
    }
    
    // 사용자 정보 가져오기
    fetchUserData();
  }, []);

  const fetchUserData = async () => {
    try {
      const response = await fetch('/api/auth/user', {
        credentials: 'include' // 쿠키 포함
      });

      if (response.ok) {
        const user = await response.json();
        setUserData(user);
        

        
        // 신규 회원인지 확인
        if (user.isNewUser) {
          console.log('New user detected, checking onboarding status');
          // 온보딩 완료 여부를 백엔드에서 확인 (user.onboardingCompleted 필드 사용)
          if (!user.onboardingCompleted) {
            console.log('Onboarding not completed, redirecting to onboarding');
            navigate('/onboarding');
            return;
          } else {
            console.log('Onboarding completed, staying on dashboard');
          }
        }
      } else {
        // 인증 실패 시 로그인 페이지로 이동
        console.log('Authentication failed, redirecting to login');
        navigate('/');
      }
    } catch (error) {
      console.error('사용자 정보 가져오기 실패:', error);
      navigate('/');
    } finally {
      setIsLoading(false);
    }
  };

  const handleLogout = () => {
    // 백엔드 로그아웃 호출
    fetch('/logout', {
      method: 'POST',
      credentials: 'include'
    }).finally(() => {
      // 로그인 페이지로 이동
      navigate('/');
    });
  };

  if (isLoading) {
    return (
      <div className="loading-container">
        <div className="loading-spinner"></div>
        <p>로딩 중...</p>
      </div>
    );
  }

  return (
    <div className="dashboard-container">
      <MoodTracker />
    </div>
  );
}

export default Dashboard; 