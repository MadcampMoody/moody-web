import axios from "axios";
import React, { useState, useEffect, useRef } from "react";
import TopBar from "./components/TopBar";
import "./DiaryEditor.css";

function DiaryEditor({ selectedDate, selectedMood, initialContent = "", diary, onCancel }) {
  const [content, setContent] = useState(initialContent);
  const [showDropdown, setShowDropdown] = useState(false);
  const dropdownRef = useRef(null);

  // 음악 추천 관련 state
  const [recommendedTracks, setRecommendedTracks] = useState([]);
  const [playlistTitle, setPlaylistTitle] = useState('');
  const [loading, setLoading] = useState(false);
  const [initialLoading, setInitialLoading] = useState(true);
  const [error, setError] = useState(null);
  const [userName, setUserName] = useState('사용자');
  const playlistRef = useRef(null);

  // 배경 스타일 동적 적용
  useEffect(() => {
    // 컴포넌트 마운트 시 배경 스타일 적용
    document.body.style.background = '#f5f1e8';
    document.body.style.backgroundImage = `
      radial-gradient(circle at 20% 80%, rgba(120, 119, 108, 0.03) 0%, transparent 50%),
      radial-gradient(circle at 80% 20%, rgba(120, 119, 108, 0.03) 0%, transparent 50%),
      radial-gradient(circle at 40% 40%, rgba(120, 119, 108, 0.03) 0%, transparent 50%),
      linear-gradient(90deg, transparent 79px, rgba(120, 119, 108, 0.04) 79px, rgba(120, 119, 108, 0.04) 81px, transparent 81px)
    `;
    document.body.style.backgroundSize = `
      300px 300px,
      200px 200px,
      400px 400px,
      100px 100px
    `;
    document.body.style.backgroundPosition = `
      0 0,
      50px 50px,
      100px 100px,
      0 0
    `;

    // 컴포넌트 언마운트 시 배경 스타일 제거 (cleanup 함수)
    return () => {
      document.body.style.background = '';
      document.body.style.backgroundImage = '';
      document.body.style.backgroundSize = '';
      document.body.style.backgroundPosition = '';
    };
  }, []); // 빈 배열을 전달하여 컴포넌트가 마운트/언마운트될 때 한 번만 실행

  // 외부 클릭 시 dropdown 닫기
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setShowDropdown(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, []);

  // 사용자 정보 가져오기 및 기존 플레이리스트 조회 (병렬 처리)
  useEffect(() => {
    const fetchData = async () => {
        try {
            // 사용자 정보와 플레이리스트 조회를 병렬로 처리
            const userInfoPromise = axios.get('/api/auth/user', { withCredentials: true });
            
            // 사용자 정보 먼저 처리
            const userResponse = await userInfoPromise;
            
            if (userResponse.data) {
                setUserName(userResponse.data.name || '사용자');
                
                // 해당 날짜의 기존 플레이리스트 조회
                if (selectedDate && userResponse.data.id) {
                    console.log(`기존 플레이리스트 조회 시작: 사용자 ID ${userResponse.data.id}, 날짜 ${selectedDate}`);
                    
                    // 플레이리스트 조회를 즉시 시작
                    const playlistResponse = await axios.get(
                        `/api/playlists/user/${userResponse.data.id}/date/${selectedDate}`, 
                        { withCredentials: true }
                    );
                    
                    console.log('플레이리스트 응답:', playlistResponse.data);
                    
                    if (playlistResponse.data && playlistResponse.data.musicList && playlistResponse.data.musicList.length > 0) {
                        console.log('기존 플레이리스트 발견, 표시 중...');
                        // 기존 플레이리스트가 있다면 표시
                        const tracks = playlistResponse.data.musicList.map(music => ({
                            trackId: music.musicUrl.split('/track/')[1]?.split('?')[0],
                            title: "Spotify Track",
                            artist: "Artist",
                            spotifyUrl: music.musicUrl,
                            previewUrl: null
                        }));
                        setRecommendedTracks(tracks);
                        setPlaylistTitle(playlistResponse.data.playlistTitle || `${selectedDate}의 플레이리스트`);
                    } else {
                        console.log('해당 날짜에 기존 플레이리스트 없음');
                    }
                }
            }
        } catch (error) {
            console.error('데이터 로딩 실패:', error);
        } finally {
            setInitialLoading(false);
        }
    };
    
    if (selectedDate) {
        fetchData();
    }
  }, [selectedDate]);

  // 추천 결과가 로드되면 해당 위치로 스크롤 (기존 플레이리스트일 때는 부드럽게, 새 추천일 때는 즉시)
  useEffect(() => {
    if (recommendedTracks.length > 0 && playlistRef.current) {
        // 로딩 상태가 아닐 때는 기존 플레이리스트이므로 부드럽게 스크롤
        // 로딩 상태일 때는 새로운 추천이므로 약간의 지연 후 스크롤
        if (!loading) {
            playlistRef.current.scrollIntoView({ behavior: 'smooth', block: 'start' });
        } else {
            setTimeout(() => {
                if (playlistRef.current) {
                    playlistRef.current.scrollIntoView({ behavior: 'smooth', block: 'start' });
                }
            }, 100);
        }
    }
  }, [recommendedTracks, loading]);

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
      // 1. 일기 저장하고 생성된 diaryId 받기
      const diaryResponse = await axios.post('/api/diary', {
        content: content,
        date: selectedDate
      }, { withCredentials: true });

      const savedDiary = diaryResponse.data;
      if (!savedDiary || !savedDiary.id) {
        throw new Error("일기 저장 후 ID를 받아오지 못했습니다.");
      }
      const diaryId = savedDiary.id;

      // 2. 음악 추천 요청
      const recommendResponse = await axios.post('/api/groq/recommend-music', 
        { prompt: content, date: selectedDate },
        { withCredentials: true }
      );
      
      const tracks = recommendResponse.data.tracks || [];
      setRecommendedTracks(tracks);
      
      const formattedDate = selectedDate || new Date().toISOString().split('T')[0];
      const newPlaylistTitle = `${formattedDate}의 플레이리스트`;
      setPlaylistTitle(newPlaylistTitle);

      // 3. 플레이리스트 생성 요청
      if (tracks.length > 0) {
        await axios.post('/api/playlists', {
          title: newPlaylistTitle,
          diaryId: diaryId,
          date: formattedDate,
          musics: tracks.map(track => ({ musicUrl: track.spotifyUrl }))
        }, { withCredentials: true });
      }

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

      // 삭제 후 페이지 이동 또는 상태 초기화
      window.location.href = "/dashboard";
    } catch (e) {
      alert("삭제 중 오류가 발생했습니다.");
    }
  };

  return (
    <div>
      <TopBar />
      <div className="diary-editor-container">
        <div className="diary-main-paper page-content" style={{ position: "relative" }}>
          {/* 상단 바: 날짜, 감정, 저장버튼, 드롭다운 */}
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
              <div className="diary-dropdown-container" ref={dropdownRef}>
                <button 
                  className="diary-dropdown-trigger"
                  onClick={() => setShowDropdown(v => !v)}
                >
                  <img src={require('./assets/dropdown.png')} alt="메뉴" style={{ width: '24px', height: '24px' }} />
                  <span className="dropdown-dots">⋮</span>
                </button>
                {showDropdown && (
                  <div className="diary-dropdown-menu">
                    <div 
                      className="diary-dropdown-item"
                      onClick={() => {
                        setShowDropdown(false);
                        /* 수정 모드 진입 */
                      }}
                    >
                      <span className="dropdown-icon">✏️</span>
                      수정
                    </div>
                    <div 
                      className="diary-dropdown-item diary-dropdown-delete"
                      onClick={() => {
                        setShowDropdown(false);
                        handleDelete();
                      }}
                    >
                      <span className="dropdown-icon">🗑️</span>
                      삭제
                    </div>
                  </div>
                )}
              </div>
            )}
            
          {/* 저장 버튼 - 내용이 변경되었고, 비어있지 않을 때만 표시 */}
          {content.length > 0 && content !== initialContent && (
            <button className="diary-save-btn-top" onClick={handleSave} disabled={loading}>
              {loading ? (
                <span style={{ fontSize: '0.85rem' }}>...</span>
              ) : (
                <img src={require('./assets/check.png')} alt="저장" style={{ width: '24px', height: '24px' }} />
              )}
            </button>
          )}
        </div>

          {/* 일기 작성 textarea */}
          <textarea
            className="diary-textarea"
            placeholder="오늘의 일기를 작성해보세요..."
            value={content}
            onChange={(e) => setContent(e.target.value)}
            rows={8}
          />
        </div>

        {/* 테두리 밖의 요소들 */}
        {error && <p className="error-message" style={{color: 'red', marginTop: '20px', textAlign: 'center'}}>{error}</p>}

        {initialLoading && (
          <div style={{textAlign: 'center', marginTop: '20px', color: '#666'}}>
            기존 플레이리스트 확인 중...
          </div>
        )}

        {/* 플레이리스트 섹션 - 테두리 밖 */}
        {recommendedTracks.length > 0 && (
            <div ref={playlistRef} className="playlist-container" style={{marginTop: '30px', width: '100%'}}>
                <h2 className="playlist-heading">{`${userName}님의 현재 무드에 맞는 플레이리스트를 만들어봤어요!`}</h2>
                <div className="playlist-card" style={{
                    border: 'none', 
                    borderRadius: '0', 
                    padding: '15px', 
                    background: 'transparent',
                    boxShadow: 'none'
                }}>
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
    </div>
  );
}

export default DiaryEditor;