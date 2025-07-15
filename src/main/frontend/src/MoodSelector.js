import { useState, useRef, useEffect } from "react";
import "./MoodSelector.css";

function MoodSelector({ selectedDate, onClose, onMoodSelect, plusButtonRef }) {
  const [selectedMood, setSelectedMood] = useState(null);
  const [rotation, setRotation] = useState(0);
  const [isDragging, setIsDragging] = useState(false);
  const [dragStart, setDragStart] = useState({ x: 0, y: 0 });
  const [lastRotation, setLastRotation] = useState(0);
  const [plusButtonRect, setPlusButtonRect] = useState(null);

  const moods = [
    { id: 'angry', name: '짜증나요', color: '#FF5A5A', emoji: '😤' },
    { id: 'mad', name: '화나요', color: '#FF0000', emoji: '😡' },
    { id: 'tired', name: '피곤해요', color: '#808080', emoji: '😴' },
    { id: 'sad', name: '슬퍼요', color: '#800080', emoji: '😢' },
    { id: 'worried', name: '걱정돼요', color: '#FFA500', emoji: '😟' },
    { id: 'happy', name: '행복해요', color: '#FFD700', emoji: '😊' },
    { id: 'calm', name: '침착해요', color: '#4169E1', emoji: '😌' },
    { id: 'excited', name: '신나요', color: '#FF69B4', emoji: '😃' },
    { id: 'proud', name: '자랑스러워요', color: '#4B0082', emoji: '😎' },
    { id: 'grateful', name: '감사해요', color: '#00FF00', emoji: '😊' }
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

  const isFutureDate = (date) => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const compareDate = new Date(date);
    compareDate.setHours(0, 0, 0, 0);
    return compareDate > today;
  };

  // 마우스 드래그 시작
  const handleMouseDown = (e) => {
    e.preventDefault();
    setIsDragging(true);
    setDragStart({ x: e.clientX, y: e.clientY });
    setLastRotation(rotation);
  };

  // 마우스 드래그 중
  const handleMouseMove = (e) => {
    if (!isDragging) return;
    
    const deltaX = e.clientX - dragStart.x;
    const newRotation = lastRotation + (deltaX * 0.2); // 감도를 0.5에서 0.2로 낮춤
    setRotation(newRotation);
  };

  // 마우스 드래그 종료
  const handleMouseUp = () => {
    setIsDragging(false);
  };

  // 터치 이벤트 (모바일 지원)
  const handleTouchStart = (e) => {
    e.preventDefault();
    const touch = e.touches[0];
    setIsDragging(true);
    setDragStart({ x: touch.clientX, y: touch.clientY });
    setLastRotation(rotation);
  };

  const handleTouchMove = (e) => {
    if (!isDragging) return;
    
    const touch = e.touches[0];
    const deltaX = touch.clientX - dragStart.x;
    const newRotation = lastRotation + (deltaX * 0.2); // 감도를 0.5에서 0.2로 낮춤
    setRotation(newRotation);
  };

  const handleTouchEnd = () => {
    setIsDragging(false);
  };

  // 이벤트 리스너 등록
  useEffect(() => {
    if (isDragging) {
      document.addEventListener('mousemove', handleMouseMove);
      document.addEventListener('mouseup', handleMouseUp);
      document.addEventListener('touchmove', handleTouchMove);
      document.addEventListener('touchend', handleTouchEnd);
      
      return () => {
        document.removeEventListener('mousemove', handleMouseMove);
        document.removeEventListener('mouseup', handleMouseUp);
        document.removeEventListener('touchmove', handleTouchMove);
        document.removeEventListener('touchend', handleTouchEnd);
      };
    }
  }, [isDragging, dragStart, lastRotation]);

  // 현재 보이는 5개 감정 계산 (세트 단위로 변경)
  const getVisibleMoods = () => {
    const visibleMoods = [];
    // 드래그 임계값을 기준으로 세트 결정 (100px 드래그마다 세트 변경)
    const setIndex = Math.floor(Math.abs(rotation) / 100) % 2;
    const startIndex = setIndex * 5; // 0-4 또는 5-9
    
    for (let i = 0; i < 5; i++) {
      const moodIndex = startIndex + i;
      // 반바퀴(180도)에 5개 배치: 각각 45도씩 간격, -90도부터 +90도까지
      const angle = -90 + (i * 45);
      
      visibleMoods.push({
        mood: moods[moodIndex],
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
        onMouseDown={handleMouseDown}
        onTouchStart={handleTouchStart}
      >
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

        {/* 드래그 힌트 */}
        <div className="inline-drag-hint">
          드래그해서 회전
        </div>
      </div>
    </>
  );
}

export default MoodSelector; 