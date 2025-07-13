import { useState } from "react";
import "./MoodSelector.css";

function MoodSelector({ selectedDate, onClose, onMoodSelect }) {
  const [selectedMood, setSelectedMood] = useState(null);

  const moods = [
    { id: 'angry', name: '짜증나요', color: '#FF5A5A', emoji: '😤' },
    { id: 'mad', name: '화나요', color: '#D62828', emoji: '😠' },
    { id: 'tired', name: '피곤해요', color: '#455A64', emoji: '😴' },
    { id: 'sad', name: '슬퍼요', color: '#3A5BA0', emoji: '😢' },
    { id: 'worried', name: '걱정돼요', color: '#4B2F80', emoji: '😰' },
    { id: 'bored', name: '지루해요', color: '#949292', emoji: '😑' },
    { id: 'happy', name: '행복해요', color: '#FFD54F', emoji: '😊' },
    { id: 'calm', name: '편안해요', color: '#A5D6A7', emoji: '😌' },
    { id: 'excited', name: '설레요', color: '#FFB6B9', emoji: '🥰' },
    { id: 'proud', name: '뿌듯해요', color: '#FF8A65', emoji: '😎' },
    { id: 'grateful', name: '감사해요', color: '#FFF176', emoji: '🙏' }
  ];

  const handleMoodSelect = (mood) => {
    setSelectedMood(mood);
  };

  const handleConfirm = () => {
    if (selectedMood) {
      onMoodSelect(selectedDate, selectedMood);
      onClose();
    }
  };

  const handleCancel = () => {
    onClose();
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
            disabled={!selectedMood}
          >
            기록하기
          </button>
        </div>
      </div>
    </div>
  );
}

export default MoodSelector; 