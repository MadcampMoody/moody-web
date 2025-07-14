import { useParams } from "react-router-dom";
import { useEffect, useState } from "react";
import axios from "axios";
import DiaryEditor from "./DiaryEditor";

function DiaryEditorWrapper() {
  const { date } = useParams(); // "2024-07-14"
  const [loading, setLoading] = useState(true);
  const [mood, setMood] = useState(null);
  const [initialContent, setInitialContent] = useState("");
  const [diary, setDiary] = useState(null); 

  useEffect(() => {
    setLoading(true);
    axios.get(`/api/mood/${date}`, { withCredentials: true })
      .then(res => {
        // 감정 enum → 이모티콘/이름 매핑
        const moodMap = {
          ANNOYED: { emoji: "😤", name: "짜증나요" },
          ANGRY: { emoji: "😡", name: "화나요" },
          TIRED: { emoji: "😴", name: "피곤해요" },
          SAD: { emoji: "😢", name: "슬퍼요" },
          WORRIED: { emoji: "😟", name: "걱정돼요" },
          BORED: { emoji: "😒", name: "지루해요" },
          HAPPY: { emoji: "😊", name: "행복해요" },
          CALM: { emoji: "😌", name: "침착해요" },
          EXCITED: { emoji: "😃", name: "신나요" },
          PROUD: { emoji: "😎", name: "자랑스러워요" },
          THANKFUL: { emoji: "😊", name: "감사해요" }
        };

        const mood = res.data.mood
          ? moodMap[res.data.mood.moodType] // moodType이 "HAPPY" 등
          : null;

        setMood(mood); // 백엔드에서 감정 정보 반환

        // diary가 객체라면 content만 추출, 없으면 빈 문자열
        if (res.data.diary && typeof res.data.diary === "object") {
            setInitialContent(res.data.diary.content || "");
            setDiary(res.data.diary); // diary 객체 저장
          } else if (typeof res.data.diary === "string") {
            setInitialContent(res.data.diary);
            setDiary(null); // diary 없음
          } else {
            setInitialContent("");
            setDiary(null);
          }
      })
      .catch(err => {
        setMood(null);
        setInitialContent("");
        setDiary(null);
      })
      .finally(() => setLoading(false));
  }, [date]);

  if (loading) return <div>로딩 중...</div>;

  return (
    <DiaryEditor
      selectedDate={date}
      selectedMood={mood}
      initialContent={initialContent}
      diary={diary}
      // onSave, onCancel 등 props 전달
    />
  );
}

export default DiaryEditorWrapper;
