import axios from "axios";
import React, { useState, useEffect, useRef, useContext } from "react"; // useContext 추가
import TopBar from "./components/TopBar";
import "./DiaryEditor.css";
import { SpotifyContext } from "./contexts/SpotifyContext"; // SpotifyContext import
import checkIcon from './assets/check.png'; // 아이콘 import
import dropdownIcon from './assets/dropdown.png'; // 드롭다운 아이콘 import 

const HeartIcon = ({ className, onClick }) => (
  <svg className={className} onClick={onClick} width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path>
  </svg>
);

const FilledHeartIcon = ({ className, onClick }) => (
  <svg className={className} onClick={onClick} width="24" height="24" viewBox="0 0 24 24" fill="#ff4500" stroke="#ff4500" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path>
  </svg>
);

function DiaryEditor({ selectedDate, selectedMood, initialContent = "", diary, onCancel }) {
  const [content, setContent] = useState(initialContent);
  const [showDropdown, setShowDropdown] = useState(false);
  const dropdownRef = useRef(null);
  const [isEditing, setIsEditing] = useState(false); // 편집 모드 상태 추가

  // 음악 추천 관련 state
  const [recommendedTracks, setRecommendedTracks] = useState([]);
  const [playlistTitle, setPlaylistTitle] = useState('');
  const [loading, setLoading] = useState(false);
  const [initialLoading, setInitialLoading] = useState(true);
  const [error, setError] = useState(null);
  const [userName, setUserName] = useState('사용자');
  const playlistRef = useRef(null);
  const [likedTracks, setLikedTracks] = useState({});

  // Spotify Context 사용
  const { accessToken, player, isSpotifyLoggedIn } = useContext(SpotifyContext);

  // 컴포넌트가 마운트되거나 diary가 변경될 때 편집 모드 설정
  useEffect(() => {
    // diary prop이 없거나 id가 없으면 새 일기로 간주하여 편집 모드로 시작
    if (!diary || !diary.id) {
      setIsEditing(true);
    } else {
      setIsEditing(false);
    }
  }, [diary]);

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
      // 날짜 기반 플레이리스트 제목 생성
      const formattedDate = selectedDate || new Date().toISOString().split('T')[0];
      setPlaylistTitle(`${formattedDate}의 플레이리스트`);

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

  const handleLike = (trackId) => {
    setLikedTracks(prev => ({
      ...prev,
      [trackId]: !prev[trackId]
    }));
  };

  const handleAddToQueue = async (track) => {
    if (!isSpotifyLoggedIn || !accessToken) {
      alert("Spotify 로그인이 필요합니다.");
      return;
    }
    if (!track.spotifyUrl) {
      alert("유효하지 않은 곡입니다.");
      return;
    }
    const trackUri = `spotify:track:${track.spotifyUrl.split('/track/')[1]}`;
    try {
      const response = await fetch(`https://api.spotify.com/v1/me/player/queue?uri=${encodeURIComponent(trackUri)}`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${accessToken}`,
        },
      });

      if (response.ok) {
        alert(`'${track.title}'을(를) 재생목록에 추가했습니다.`);
      } else {
        const errorData = await response.json();
        alert(`큐에 추가 실패: ${errorData.error.message}`);
      }
    } catch (e) {
      alert("큐에 추가하는 중 오류 발생");
    }
  };

  // 전체 플레이리스트를 큐에 추가하는 함수
  const handleAddPlaylistToQueue = async () => {
    if (!isSpotifyLoggedIn || !accessToken) {
      alert("Spotify 로그인이 필요합니다.");
      return;
    }
    if (recommendedTracks.length === 0) {
      alert("추가할 곡이 없습니다.");
      return;
    }

    let successCount = 0;
    for (const track of recommendedTracks) {
      if (!track.spotifyUrl) continue;
      const trackUri = `spotify:track:${track.spotifyUrl.split('/track/')[1]}`;
      try {
        const response = await fetch(`https://api.spotify.com/v1/me/player/queue?uri=${encodeURIComponent(trackUri)}`, {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${accessToken}`,
          },
        });
        if (response.ok) {
          successCount++;
        }
      } catch (e) {
        // 개별 곡 추가 실패는 무시하고 계속 진행
        console.error("큐에 곡 추가 중 오류:", e);
      }
    }
    alert(`${successCount}곡을 재생목록에 추가했습니다.`);
  };

  return (
    <div>
      <TopBar />
      <div className="diary-main-paper page-content" style={{ position: "relative" }}>
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
        {diary && content && !isEditing && (
          <div className="diary-dropdown-container" ref={dropdownRef}>
            <button 
              className="diary-dropdown-trigger"
              onClick={() => setShowDropdown(v => !v)}
            >
              <img src={dropdownIcon} alt="Dropdown" />
            </button>
            {showDropdown && (
              <div className="diary-dropdown-menu">
                <div 
                  className="diary-dropdown-item"
                  onClick={() => {
                    setIsEditing(true);
                    setShowDropdown(false);
                  }}
                >
                  <span className="dropdown-icon">✏️</span>
                  수정
                </div>
              </div>
            )}
          </div>
        )}
      </div>

      {isEditing && content && (
        <div className="diary-actions">
          <button className="diary-save-btn" onClick={handleSave} disabled={loading}>
            {loading ? <div className="loader"></div> : <img src={checkIcon} alt="Save" />}
          </button>
        </div>
      )}

      <textarea
        className={`diary-textarea ${!isEditing ? 'readonly' : ''}`}
        placeholder="오늘의 일기를 작성해보세요..."
        value={content}
        onChange={(e) => setContent(e.target.value)}
        rows={8}
        readOnly={!isEditing}
      />

        {error && <p className="error-message" style={{color: 'red', marginTop: '20px', textAlign: 'center'}}>{error}</p>}

        {initialLoading && (
          <div style={{textAlign: 'center', marginTop: '20px', color: '#666'}}>
            기존 플레이리스트 확인 중...
          </div>
        )}

        {recommendedTracks.length > 0 && (
            <div ref={playlistRef} className="playlist-container" style={{marginTop: '30px', width: '100%'}}>
                <h2 className="playlist-heading">{`${userName}님의 현재 무드에 맞는 플레이리스트를 만들어봤어요!`}</h2>
                <div className="playlist-card" style={{
                    border: '1px solid #e2cdb0', 
                    borderRadius: '8px', 
                    padding: '15px', 
                    background: '#fff8f1',
                    backgroundImage: `
                        radial-gradient(circle at 25% 25%, rgba(180, 140, 120, 0.015) 0%, transparent 50%),
                        radial-gradient(circle at 75% 75%, rgba(180, 140, 120, 0.015) 0%, transparent 50%),
                        linear-gradient(0deg, transparent 0%, rgba(180, 140, 120, 0.008) 50%, transparent 100%),
                        linear-gradient(90deg, transparent 0%, rgba(180, 140, 120, 0.008) 50%, transparent 100%)
                    `,
                    backgroundSize: '100px 100px, 120px 120px, 2px 2px, 2px 2px',
                    backgroundPosition: '0 0, 30px 30px, 0 0, 0 0',
                    boxShadow: '0 2px 8px rgba(180, 140, 120, 0.1), inset 0 0 50px rgba(255, 248, 241, 0.8)'
                }}>
                  <div className="playlist-header">
                    <h3>{playlistTitle}</h3>
                    <button
                      onClick={handleAddPlaylistToQueue}
                      style={{
                        backgroundColor: '#1DB954',
                        color: 'white',
                        padding: '8px 16px',
                        borderRadius: '50px',
                        textDecoration: 'none',
                        display: 'flex',
                        alignItems: 'center',
                        fontWeight: 'bold',
                        fontSize: '0.9rem',
                        border: 'none',
                        cursor: 'pointer'
                      }}
                    >
                       Spotify에서 듣기
                       <img src="/spotify-logo.svg" alt="Spotify" style={{width: '40px', marginLeft: '8px'}}/>
                    </button>
                  </div>
                  <ul className="playlist-list">
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
                    </ul>
                </div>
            </div>
        )}
      </div>
    </div>
  );
}

export default DiaryEditor;