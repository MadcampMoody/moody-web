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
    // 백엔드에서 사용자 데이터 가져오기
    fetchUserData();
    // 현재 월의 감정 기록 가져오기
    fetchMoodRecords();
  }, [currentDate]); // currentDate가 변경될 때마다 감정 기록 다시 가져오기

  const fetchUserData = async () => {
    try {
      const response = await fetch('/api/auth/user', {
        credentials: 'include'
      });
      if (response.ok) {
        const user = await response.json();
        setUserData(user);
      }
    } catch (error) {
      console.error('사용자 정보 가져오기 실패:', error);
    }
  };

  const fetchMoodRecords = async () => {
    try {
      const year = currentDate.getFullYear();
      const month = currentDate.getMonth() + 1; // JavaScript month는 0부터 시작
      console.log(`감정 기록 조회 시작: ${year}년 ${month}월`);
      
      const response = await fetch(`/api/mood/month?year=${year}&month=${month}`, {
        credentials: 'include'
      });
      
      console.log('API 응답 상태:', response.status);
      
      if (response.ok) {
        const monthlyMoods = await response.json();
        console.log('백엔드에서 받은 데이터:', monthlyMoods);
        
        // 백엔드에서 받은 데이터를 moodRecords 형태로 변환
        const moodRecordsMap = {};
        monthlyMoods.forEach(mood => {
          const date = new Date(mood.date);
          const dateKey = date.toDateString();
          console.log(`날짜 변환: ${mood.date} -> ${dateKey}`);
          
          moodRecordsMap[dateKey] = {
            id: getMoodId(mood.moodType),
            emoji: getMoodEmoji(mood.moodType),
            name: getMoodName(mood.moodType),
            color: getMoodColor(mood.moodType)
          };
        });
        
        console.log('변환된 moodRecords:', moodRecordsMap);
        setMoodRecords(moodRecordsMap);
      } else {
        console.error('API 호출 실패:', response.status, response.statusText);
      }
    } catch (error) {
      console.error('감정 기록 가져오기 실패:', error);
    }
  };

  // 감정 타입을 이모지로 변환
  const getMoodEmoji = (moodType) => {
    const moodMap = {
      ANNOYED: "😤",
      ANGRY: "😡",
      TIRED: "😴",
      SAD: "😢",
      WORRIED: "😟",
      BORED: "😒",
      HAPPY: "😊",
      CALM: "😌",
      EXCITED: "😃",
      PROUD: "😎",
      THANKFUL: "😊"
    };
    return moodMap[moodType] || "😐";
  };

  // 감정 타입을 이름으로 변환
  const getMoodName = (moodType) => {
    const moodMap = {
      ANNOYED: "짜증나요",
      ANGRY: "화나요",
      TIRED: "피곤해요",
      SAD: "슬퍼요",
      WORRIED: "걱정돼요",
      BORED: "지루해요",
      HAPPY: "행복해요",
      CALM: "침착해요",
      EXCITED: "신나요",
      PROUD: "자랑스러워요",
      THANKFUL: "감사해요"
    };
    return moodMap[moodType] || "보통";
  };

  // 감정 타입을 ID로 변환
  const getMoodId = (moodType) => {
    const moodMap = {
      ANNOYED: "angry",
      ANGRY: "mad",
      TIRED: "tired",
      SAD: "sad",
      WORRIED: "worried",
      BORED: "bored",
      HAPPY: "happy",
      CALM: "calm",
      EXCITED: "excited",
      PROUD: "proud",
      THANKFUL: "grateful"
    };
    return moodMap[moodType] || "happy";
  };

  // 감정 타입을 색상으로 변환
  const getMoodColor = (moodType) => {
    const moodMap = {
      ANNOYED: "#FF5A5A",
      ANGRY: "#FF0000",
      TIRED: "#808080",
      SAD: "#800080",
      WORRIED: "#FFA500",
      BORED: "#008000",
      HAPPY: "#FFD700",
      CALM: "#4169E1",
      EXCITED: "#FF69B4",
      PROUD: "#4B0082",
      THANKFUL: "#00FF00"
    };
    return moodMap[moodType] || "#FFD700";
  };

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

  const handleMoodSelect = async (date, mood) => {
    try {
      const dateString = formatDateToYYYYMMDD(date);
      
      // mood.id를 enum 값으로 변환
      const moodEnumMap = {
        angry: "ANNOYED",
        mad: "ANGRY",
        tired: "TIRED",
        sad: "SAD",
        worried: "WORRIED",
        bored: "BORED",
        happy: "HAPPY",
        calm: "CALM",
        excited: "EXCITED",
        proud: "PROUD",
        grateful: "THANKFUL"
      };
      
      const response = await fetch('/api/mood', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        credentials: 'include',
        body: JSON.stringify({
          date: dateString,
          moodType: moodEnumMap[mood.id] // mood.id를 enum 값으로 변환
        })
      });

      if (response.ok) {
        // 성공적으로 저장되면 로컬 상태 업데이트
        const dateKey = date.toDateString();
        const newMoodRecords = {
          ...moodRecords,
          [dateKey]: {
            id: mood.id,
            emoji: mood.emoji,
            name: mood.name,
            color: mood.color
          }
        };
        setMoodRecords(newMoodRecords);
        setShowMoodSelector(false);
      } else {
        console.error('감정 저장 실패');
        alert('감정 저장에 실패했습니다.');
      }
    } catch (error) {
      console.error('감정 저장 중 오류:', error);
      alert('감정 저장 중 오류가 발생했습니다.');
    }
  };

  const handleLogout = () => {
    // 백엔드 로그아웃 호출
    fetch('/logout', {
      method: 'POST',
      credentials: 'include'
    }).finally(() => {
      window.location.href = '/';
    });
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