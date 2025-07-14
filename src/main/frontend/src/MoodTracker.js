import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import "./MoodTracker.css";
import MoodSelector from "./MoodSelector";

function MoodTracker() {
  const [currentDate, setCurrentDate] = useState(new Date());
  const [selectedDate, setSelectedDate] = useState(null);
  const [userData, setUserData] = useState(null);
  const [showMoodSelector, setShowMoodSelector] = useState(false);
  const [moodRecords, setMoodRecords] = useState({});
  const navigate = useNavigate();

  useEffect(() => {
    // 로컬 스토리지에서 사용자 데이터 가져오기
    const savedUserData = localStorage.getItem('userData');
    if (savedUserData) {
      setUserData(JSON.parse(savedUserData));
    }

    // 저장된 감정 기록 가져오기
    const savedMoodRecords = localStorage.getItem('moodRecords');
    if (savedMoodRecords) {
      setMoodRecords(JSON.parse(savedMoodRecords));
    }
  }, []);

  // 현재 월의 첫 번째 날과 마지막 날 계산
  const firstDayOfMonth = new Date(currentDate.getFullYear(), currentDate.getMonth(), 1);
  const lastDayOfMonth = new Date(currentDate.getFullYear(), currentDate.getMonth() + 1, 0);
  const startDate = new Date(firstDayOfMonth);
  startDate.setDate(startDate.getDate() - firstDayOfMonth.getDay());

  // 캘린더에 표시할 날짜들 생성
  const calendarDays = [];
  const endDate = new Date(lastDayOfMonth);
  endDate.setDate(endDate.getDate() + (6 - lastDayOfMonth.getDay()));

  for (let d = new Date(startDate); d <= endDate; d.setDate(d.getDate() + 1)) {
    calendarDays.push(new Date(d));
  }

  const monthNames = [
    "1월", "2월", "3월", "4월", "5월", "6월",
    "7월", "8월", "9월", "10월", "11월", "12월"
  ];

  const dayNames = ["일", "월", "화", "수", "목", "금", "토"];

  const goToPreviousMonth = () => {
    setCurrentDate(new Date(currentDate.getFullYear(), currentDate.getMonth() - 1, 1));
  };

  const goToNextMonth = () => {
    setCurrentDate(new Date(currentDate.getFullYear(), currentDate.getMonth() + 1, 1));
  };

  const goToToday = () => {
    setCurrentDate(new Date());
    setSelectedDate(new Date());
  };

  const handleDateClick = (date) => {
    const dateKey = date.toDateString();
    // 감정이 등록된 날짜만 일기장으로 이동
    if (moodRecords[dateKey]) {
      // 예: React Router 사용 시
      navigate(`/diary/${formatDateToYYYYMMDD(date)}`);
    } else {
      setSelectedDate(date); // 기존 선택 로직
    }
  };

  const handleAddMoodClick = () => {
    setShowMoodSelector(true);
  };

  const handleMoodSelect = (date, mood) => {
    const dateKey = date.toDateString();
    const newMoodRecords = {
      ...moodRecords,
      [dateKey]: mood
    };
    setMoodRecords(newMoodRecords);
    localStorage.setItem('moodRecords', JSON.stringify(newMoodRecords));
  };

  const handleLogout = () => {
    // 로그아웃 처리
    localStorage.removeItem('onboardingCompleted');
    localStorage.removeItem('userData');
    localStorage.removeItem('authToken');
    localStorage.removeItem('moodRecords');
    window.location.href = '/';
  };

  const isToday = (date) => {
    const today = new Date();
    return date.toDateString() === today.toDateString();
  };

  const isCurrentMonth = (date) => {
    return date.getMonth() === currentDate.getMonth();
  };

  const isSelected = (date) => {
    return selectedDate && date.toDateString() === selectedDate.toDateString();
  };

  const getMoodForDate = (date) => {
    const dateKey = date.toDateString();
    return moodRecords[dateKey];
  };

  const isFutureDate = (date) => {
    const today = new Date();
    // 시/분/초/밀리초 제거
    today.setHours(0, 0, 0, 0);
    const compareDate = new Date(date);
    compareDate.setHours(0, 0, 0, 0);
    return compareDate > today;
  };

  const formatDateToYYYYMMDD = (date) => {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
  };

  return (
    <div className="mood-tracker-container">
      <div className="mood-tracker-header">
        <div className="header-content">
          <div className="user-welcome">
            <h1>안녕하세요, {userData?.name || '사용자'}님!</h1>
            <p>오늘 하루는 어떠셨나요?</p>
          </div>
          <div className="header-actions">
            <button className="today-btn" onClick={() => navigate('/music_recommend')}>
              음악 추천
            </button>
            <button className="today-btn" onClick={goToToday}>
              오늘
            </button>
            <button className="logout-btn" onClick={handleLogout}>
              로그아웃
            </button>
          </div>
        </div>
      </div>

      <div className="calendar-container">
        <div className="calendar-header">
          <button className="nav-btn" onClick={goToPreviousMonth}>
            ‹
          </button>
          <h2 className="month-year">
            {currentDate.getFullYear()}년 {monthNames[currentDate.getMonth()]}
          </h2>
          <button className="nav-btn" onClick={goToNextMonth}>
            ›
          </button>
        </div>

        <div className="calendar">
          <div className="calendar-weekdays">
            {dayNames.map((day, index) => (
              <div key={index} className="weekday">
                {day}
              </div>
            ))}
          </div>

          <div className="calendar-days">
            {calendarDays.map((date, index) => {
              const mood = getMoodForDate(date);
              return (
                <div
                  key={index}
                  className={`calendar-day ${
                    !isCurrentMonth(date) ? 'other-month' : ''
                  } ${isToday(date) ? 'today' : ''} ${
                    isSelected(date) ? 'selected' : ''
                  }`}
                  onClick={() => handleDateClick(date)}
                >
                  <span className="day-number">{date.getDate()}</span>
                  <div 
                    className="mood-indicator"
                    style={{
                      backgroundColor: mood ? mood.color : 'transparent',
                      border: mood ? `2px solid ${mood.color}` : 'none'
                    }}
                  >
                    {mood && <span style={{ fontSize: '10px' }}>{mood.emoji}</span>}
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </div>

      {selectedDate && (
        <div className="selected-date-info">
          <h3>
            {selectedDate.getFullYear()}년 {selectedDate.getMonth() + 1}월 {selectedDate.getDate()}일
          </h3>
          <button
            className="add-mood-btn"
            onClick={handleAddMoodClick}
            disabled={isFutureDate(selectedDate)} // 미래면 비활성화
            style={isFutureDate(selectedDate) ? { opacity: 0.5, cursor: "not-allowed" } : {}}
          >
            <span className="plus-icon">+</span>
            <span className="btn-text">감정 기록하기</span>
          </button>
          {isFutureDate(selectedDate) && (
            <div style={{ color: "red", marginTop: "8px", fontSize: "13px" }}>
              미래 날짜에는 감정을 기록할 수 없습니다.
            </div>
          )}
        </div>
      )}

      {showMoodSelector && (
        <MoodSelector
          selectedDate={selectedDate}
          onClose={() => setShowMoodSelector(false)}
          onMoodSelect={handleMoodSelect}
        />
      )}
    </div>
  );
}

export default MoodTracker; 