import { useState } from "react";
import "./MoodSelector.css";
import axios from "axios";

function formatDateToYYYYMMDD(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function MoodSelector({ selectedDate, onClose, onMoodSelect }) {
  const [selectedMood, setSelectedMood] = useState(null);

  const moods = [
    { id: 'angry', name: '짜증나요', color: '#FF5A5A', emoji: '😤' },
    { id: 'mad', name: '화나요', color: '#FF0000', emoji: '😡' },
    { id: 'tired', name: '피곤해요', color: '#808080', emoji: '😴' },
    { id: 'sad', name: '슬퍼요', color: '#800080', emoji: '😢' },
    { id: 'worried', name: '걱정돼요', color: '#FFA500', emoji: '😟' },
    { id: 'bored', name: '지루해요', color: '#008000', emoji: '😒' },
    { id: 'happy', name: '행복해요', color: '#FFD700', emoji: '😊' },
    { id: 'calm', name: '침착해요', color: '#4169E1', emoji: '😌' },
    { id: 'excited', name: '신나요', color: '#FF69B4', emoji: '😃' },
    { id: 'proud', name: '자랑스러워요', color: '#4B0082', emoji: '😎' },
    { id: 'grateful', name: '감사해요', color: '#00FF00', emoji: '😊' }
  ];

  // 한글 → 영문 enum 변환 맵
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

  const handleMoodSelect = (mood) => {
    setSelectedMood(mood);
  };

  const handleConfirm = async () => {
    if (selectedMood) {
      // 1. 로컬 기록
      onMoodSelect(selectedDate, selectedMood);

      // 2. 백엔드로 기록
      try {
        await axios.post(
          "/api/mood",
          {
            date: formatDateToYYYYMMDD(selectedDate),
            mood: moodEnumMap[selectedMood.id]
          },
          { withCredentials: true }
        );
        // 성공 알림 등 추가 가능
      } catch (e) {
        alert("서버에 감정 기록 저장 실패: " + (e.response?.data?.message || e.message));
      }

      onClose();
    }
  };

  const handleCancel = () => {
    onClose();
  };

  const isFutureDate = (date) => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const compareDate = new Date(date);
    compareDate.setHours(0, 0, 0, 0);
    return compareDate > today;
  };

  return (
    <div className="mood-selector-overlay">
      <div className="mood-selector-modal">
        <div className="mood-selector-header">
          <h2>
            {selectedDate.getFullYear()}년 {selectedDate.getMonth() + 1}월 {selectedDate.getDate()}일
          </h2>
          <p>오늘 하루는 어떠셨나요?</p>
          <button className="close-btn" onClick={handleCancel}>
            ×
          </button>
        </div>

        <div className="mood-grid">
          {moods.map((mood) => (
            <div
              key={mood.id}
              className={`mood-sticker ${selectedMood?.id === mood.id ? 'selected' : ''}`}
              onClick={() => handleMoodSelect(mood)}
              style={{ '--mood-color': mood.color }}
            >
              <div className="mood-emoji">{mood.emoji}</div>
              <div className="mood-name">{mood.name}</div>
            </div>
          ))}
        </div>

        <div className="mood-selector-actions">
          <button className="cancel-btn" onClick={handleCancel}>
            취소
          </button>
          <button 
            className="confirm-btn" 
            onClick={handleConfirm}
            disabled={!selectedMood || isFutureDate(selectedDate)}
          >
            기록하기
          </button>
          {isFutureDate(selectedDate) && (
            <div style={{ color: "red", marginTop: "8px", fontSize: "13px" }}>
              미래 날짜에는 감정을 기록할 수 없습니다.
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

export default MoodSelector; 