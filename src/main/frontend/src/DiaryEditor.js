import axios from "axios";
import React, { useState } from "react";
import "./DiaryEditor.css";

function DiaryEditor({ selectedDate, selectedMood, initialContent = "", onCancel }) {
  const [content, setContent] = useState(initialContent);

  // 날짜 포맷 함수
  const formatDate = (dateString) => {
    if (!dateString) return "";
    const [year, month, day] = dateString.split("-");
    return `${year}년 ${parseInt(month)}월 ${parseInt(day)}일`;
  };

  const handleSave = async () => {
    try {
      await axios.post('/api/diary', {
        content: content,
        date: selectedDate // "2024-07-14" 등
      }, { withCredentials: true });
      alert("일기가 저장되었습니다!");
      // 저장 후 이동/닫기 등 추가 동작
    } catch (e) {
      alert("저장 실패: " + (e.response?.data?.message || e.message));
    }
  };

  return (
    <div className="diary-editor-container">
      <div className="diary-header">
        <div className="diary-date">{formatDate(selectedDate)}</div>
        <div className="diary-mood-center">
          {selectedMood && (
            <>
              <span className="diary-mood-emoji" style={{ fontSize: "2rem" }}>
                {selectedMood.emoji}
              </span>
              <span className="diary-mood-name" style={{ marginLeft: "8px", fontWeight: 600 }}>
                {selectedMood.name}
              </span>
            </>
          )}
        </div>
      </div>
      <textarea
        className="diary-textarea"
        placeholder="오늘의 일기를 작성해보세요..."
        value={content}
        onChange={(e) => setContent(e.target.value)}
        rows={8}
      />
      <div className="diary-actions">
        <button className="diary-save-btn" onClick={handleSave}>
          저장
        </button>
        <button className="diary-cancel-btn" onClick={onCancel}>
          취소
        </button>
      </div>
    </div>
  );
}

export default DiaryEditor;