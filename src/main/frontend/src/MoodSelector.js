import { useState, useRef, useEffect } from "react";
import "./MoodSelector.css";

function MoodSelector({ selectedDate, onClose, onMoodSelect, plusButtonRef }) {
  const [selectedMood, setSelectedMood] = useState(null);
  const [currentSet, setCurrentSet] = useState(0); // 0 또는 1
  const [plusButtonRect, setPlusButtonRect] = useState(null);

  // 긍정적인 감정들 (먼저 보여짐)
  const positiveMoods = [
    { id: 'happy', name: '행복해요', color: '#FFD700', emoji: '😊' },
    { id: 'excited', name: '신나요', color: '#FF69B4', emoji: '😃' },
    { id: 'calm', name: '침착해요', color: '#4169E1', emoji: '😌' },
    { id: 'proud', name: '자랑스러워요', color: '#4B0082', emoji: '😎' },
    { id: 'grateful', name: '감사해요', color: '#00FF00', emoji: '🙏' }
  ];

  // 부정적인 감정들 (변경 버튼으로 전환)
  const negativeMoods = [
    { id: 'angry', name: '짜증나요', color: '#FF5A5A', emoji: '😤' },
    { id: 'mad', name: '화나요', color: '#FF0000', emoji: '😡' },
    { id: 'sad', name: '슬퍼요', color: '#800080', emoji: '😢' },
    { id: 'tired', name: '피곤해요', color: '#808080', emoji: '😴' },
    { id: 'worried', name: '걱정돼요', color: '#FFA500', emoji: '😟' }
  ];

  // + 버튼 위치 계산
  useEffect(() => {
    const updatePlusButtonPosition = () => {
      if (plusButtonRef?.current) {
        const rect = plusButtonRef.current.getBoundingClientRect();
        setPlusButtonRect({
          top: rect.top,
          left: rect.left,
          width: rect.width,
          height: rect.height
        });
      }
    };

    // 초기 위치 설정
    updatePlusButtonPosition();

    // 스크롤 이벤트 리스너 추가
    window.addEventListener('scroll', updatePlusButtonPosition);
    window.addEventListener('resize', updatePlusButtonPosition);

    return () => {
      window.removeEventListener('scroll', updatePlusButtonPosition);
      window.removeEventListener('resize', updatePlusButtonPosition);
    };
  }, [plusButtonRef]);

  const handleMoodSelect = (mood) => {
    setSelectedMood(mood);
    // 감정 선택 시 바로 기록
    onMoodSelect(selectedDate, mood);
    onClose();
  };

  const handleSetChange = () => {
    setCurrentSet(prev => (prev + 1) % 2); // 0과 1 사이에서 토글
  };

  const isFutureDate = (date) => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const compareDate = new Date(date);
    compareDate.setHours(0, 0, 0, 0);
    return compareDate > today;
  };

  // 현재 보이는 5개 감정 계산 (긍정/부정 감정 세트로 변경)
  const getVisibleMoods = () => {
    const visibleMoods = [];
    // currentSet 0: 긍정적인 감정, currentSet 1: 부정적인 감정
    const currentMoods = currentSet === 0 ? positiveMoods : negativeMoods;
    
    for (let i = 0; i < 5; i++) {
      // 반바퀴(180도)에 5개 배치: 각각 45도씩 간격, -90도부터 +90도까지
      const angle = -90 + (i * 45);
      
      visibleMoods.push({
        mood: currentMoods[i],
        angle: angle,
        index: i
      });
    }
    
    return visibleMoods;
  };

  const visibleMoods = getVisibleMoods();

  if (!plusButtonRect) return null;

  return (
    <>
      {/* 배경 블러 오버레이 */}
      <div className="inline-mood-overlay" onClick={onClose} />
      
      {/* + 버튼 위치에 고정된 감정 선택기 */}
      <div 
        className="inline-mood-container"
        style={{
          top: plusButtonRect.top + plusButtonRect.height / 2,
          left: plusButtonRect.left + plusButtonRect.width / 2,
        }}
      >
        {/* 중앙의 변경 버튼 */}
        <div 
          className="change-btn" 
          onClick={handleSetChange}
          title="다른 감정 보기"
        >
          <img src={require('./assets/change-icon.png')} alt="change" className="change-icon-svg" />
        </div>

        {/* 원형으로 배치된 감정들 */}
        {visibleMoods.map(({ mood, angle, index }) => (
          <div
            key={`${mood.id}-${index}`}
            className="inline-mood-item"
            style={{
              '--angle': `${angle}deg`,
              '--mood-color': mood.color
            }}
            onClick={() => handleMoodSelect(mood)}
          >
            <div className="mood-emoji">{mood.emoji}</div>
          </div>
        ))}

        {/* 변경 힌트 */}
        <div className="inline-change-hint">
          {currentSet === 0 ? '긍정적인 감정들 → 부정적인 감정 보기' : '부정적인 감정들 → 긍정적인 감정 보기'}
        </div>
      </div>
    </>
  );
}

export default MoodSelector; 