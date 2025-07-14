import axios from "axios";
import React, { useState, useEffect, useRef } from "react";
import "./DiaryEditor.css";

function DiaryEditor({ selectedDate, selectedMood, initialContent = "", diary, onCancel }) {
  const [content, setContent] = useState(initialContent);
  const [showDropdown, setShowDropdown] = useState(false);

  // 음악 추천 관련 state
  const [recommendedTracks, setRecommendedTracks] = useState([]);
  const [playlistTitle, setPlaylistTitle] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [userName, setUserName] = useState('사용자');
  const playlistRef = useRef(null);

  // 사용자 정보 가져오기
  useEffect(() => {
    const fetchUserInfo = async () => {
        try {
            const response = await axios.get('/api/auth/user', { withCredentials: true });
            if (response.data) {
                setUserName(response.data.name || '사용자');
            }
        } catch (error) {
            console.error('사용자 정보 로딩 실패:', error);
        }
    };
    fetchUserInfo();
  }, []);

  // 추천 결과가 로드되면 해당 위치로 스크롤
  useEffect(() => {
    if (recommendedTracks.length > 0 && playlistRef.current) {
        playlistRef.current.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  }, [recommendedTracks]);

  // 변경: "7/8"만 반환
  const formatShortDate = (dateString) => {
    if (!dateString) return "";
    const [year, month, day] = dateString.split("-");
    return `${parseInt(month)}/${parseInt(day)}`;
  };

  const handleSave = async () => {
    setLoading(true);
    setError(null);
    setRecommendedTracks([]);
    setPlaylistTitle('');

    try {
      // 1. 일기 저장 (내용이 비어있어도 저장)
      await axios.post('/api/diary', {
        content: content,
        date: selectedDate
      }, { withCredentials: true });

      // 2. 음악 추천 요청 (date 필드 추가)
      const response = await axios.post('/api/groq/recommend-music', 
        { prompt: content, date: selectedDate },
        { withCredentials: true }
      );
      
      setRecommendedTracks(response.data.tracks || []);
      setPlaylistTitle(response.data.playlistTitle || '나를 위한 무드 플레이리스트');

    } catch (e) {
      setError("요청 처리 중 오류가 발생했습니다: " + (e.response?.data?.message || e.message));
      alert("오류가 발생했습니다: " + (e.response?.data?.message || e.message));
    } finally {
      setLoading(false);
    }
  };

  // 삭제 핸들러
  const handleDelete = async () => {
    if (!diary || !diary.id) {
      alert("삭제할 일기 정보가 없습니다.");
      return;
    }
    if (!window.confirm("정말 삭제하시겠습니까?")) return;
    try {
      await axios.post('/api/diary/delete', { diaryId: diary.id }, { withCredentials: true });

      // === 여기에서 localStorage 데이터도 삭제 ===
      localStorage.removeItem(`mood-${selectedDate}`);
      localStorage.removeItem(`diary-${selectedDate}`);

      // 삭제 후 페이지 이동 또는 상태 초기화
      window.location.href = "/dashboard";
    } catch (e) {
      alert("삭제 중 오류가 발생했습니다.");
    }
  };

  return (
    <div style={{ padding: '20px', maxWidth: '800px', margin: '0 auto' }}>
    <div className="diary-main-paper" style={{ position: "relative" }}>
      {/* 상단 바: 날짜, 감정, 드롭다운 */}
      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", position: "relative" }}>
        {/* 날짜 (왼쪽) */}
        <div className="diary-date">{formatShortDate(selectedDate)}</div>
        {/* 감정 (가운데) */}
        <div className="diary-mood-center">
          {selectedMood && (
            <>
              <span className="diary-mood-emoji">{selectedMood.emoji}</span>
              <span className="diary-mood-name">{selectedMood.name}</span>
            </>
          )}
        </div>
        {/* 드롭다운 (오른쪽) */}
        {diary && content && (
          <div style={{ position: "absolute", top: 20, right: 20 }}>
            <button onClick={() => setShowDropdown(v => !v)}>⋮</button>
            {showDropdown && (
              <div style={{ position: "absolute", right: 0, background: "#fff", border: "1px solid #ccc" }}>
                <div onClick={() => {/* 수정 모드 진입 */}}>수정</div>
                <div onClick={handleDelete}>삭제</div>
              </div>
            )}
          </div>
        )}
      </div>
          
          <textarea
            className="diary-textarea"
            placeholder="오늘의 일기를 작성해보세요..."
            value={content}
            onChange={(e) => setContent(e.target.value)}
            rows={8}
          />
          <div className="diary-actions">
            <button className="diary-save-btn" onClick={handleSave} disabled={loading}>
              {loading ? '추천받는 중...' : '저장 및 음악 추천받기'}
            </button>
            <button className="diary-cancel-btn" onClick={onCancel} disabled={loading}>
              취소
            </button>
          </div>
        </div>

        {error && <p className="error-message" style={{color: 'red', marginTop: '20px', textAlign: 'center'}}>{error}</p>}

        {recommendedTracks.length > 0 && (
            <div ref={playlistRef} className="playlist-container" style={{marginTop: '30px', width: '100%'}}>
                <h2 className="playlist-heading">{`${userName}님의 현재 무드에 맞는 플레이리스트를 만들어봤어요!`}</h2>
                <div className="playlist-card" style={{border: '1px solid #ddd', borderRadius: '8px', padding: '15px', background: '#f9f9f9'}}>
                    <div className="playlist-header" style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '15px'}}>
                        <div className="header-title" style={{fontWeight: 'bold', fontSize: '1.2rem'}}>{playlistTitle}</div>
                        <div className="spotify-link">
                            <a 
                              href="https://open.spotify.com" 
                              target="_blank" 
                              rel="noopener noreferrer" 
                              style={{
                                backgroundColor: '#1DB954',
                                color: 'white',
                                padding: '8px 16px',
                                borderRadius: '50px',
                                textDecoration: 'none',
                                display: 'flex',
                                alignItems: 'center',
                                fontWeight: 'bold',
                                fontSize: '0.9rem'
                              }}
                            >
                                Spotify에서 듣기
                                <img src="/spotify-logo.svg" alt="Spotify" style={{width: '40px', marginLeft: '8px'}}/>
                            </a>
                        </div>
                    </div>
                    <div className="playlist-body">
                        {recommendedTracks.map((track, index) => (
                            <div key={track.trackId || index} className="playlist-track" style={{display: 'flex', alignItems: 'center', marginBottom: '10px'}}>
                                <span style={{marginRight: '15px', fontWeight: 'bold', fontSize: '1.1rem', minWidth: '20px', textAlign: 'center'}}>{index + 1}</span>
                                <div style={{width: '100%'}}>
                                  <iframe
                                      src={`https://open.spotify.com/embed/track/${track.trackId}?utm_source=generator&theme=0`}
                                      width="100%"
                                      height="80"
                                      frameBorder="0"
                                      allowFullScreen=""
                                      allow="autoplay; clipboard-write; encrypted-media; fullscreen; picture-in-picture"
                                      loading="lazy"
                                      title={`${track.title} by ${track.artist}`}
                                  ></iframe>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            </div>
        )}
      </div>
    // </div>
  );
}

export default DiaryEditor;