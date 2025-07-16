import axios from "axios";
import React, { useState, useEffect, useRef, useContext } from "react";
import "./DiaryEditor.css";
import spotifyPlayerService from './services/SpotifyPlayerService';
import { SpotifyPlayerContext } from "./App";

function DiaryEditor({ selectedDate, selectedMood, initialContent = "", diary, onCancel }) {
  // Spotify 관련 기능에만 사용, 무드/일기 저장과는 무관하게 분리
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
  const [likedTrackIds, setLikedTrackIds] = useState(new Set());
  const playlistRef = useRef(null);

  useEffect(() => {
    window.scrollTo(0, 0);
  }, []);

  // 배경 스타일 동적 적용

  useEffect(() => {
    window.scrollTo(0, 0);
  }, []);
  
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
    if (loading) return;
    setLoading(true);
    setError(null);
    setRecommendedTracks([]);
    setPlaylistTitle('');

    try {
      // 1. 일기 저장 또는 업데이트
      const diaryResponse = await axios.post('/api/diary', {
        id: diary ? diary.id : null, // 기존 일기 ID 전달
        content: content,
        date: selectedDate
      }, { withCredentials: true });

      const savedDiary = diaryResponse.data;
      if (!savedDiary || !savedDiary.id) {
        throw new Error("일기 저장 후 ID를 받아오지 못했습니다.");
      }
      
      alert("일기가 저장되었습니다!");

      // 음악 추천은 항상 실행
      setLoading(true);
      try {
        const recommendResponse = await axios.post('/api/groq/recommend-music', 
          { prompt: content, date: selectedDate },
          { withCredentials: true }
        );
        
        const tracks = recommendResponse.data.tracks || [];
        setRecommendedTracks(tracks);
        
        const formattedDate = selectedDate || new Date().toISOString().split('T')[0];
        const newPlaylistTitle = `${formattedDate}의 플레이리스트`;
        setPlaylistTitle(newPlaylistTitle);

      } catch (musicError) {
        console.error("음악 추천 실패:", musicError);
        alert("음악 추천 중 오류가 발생했습니다.");
      } finally {
        setLoading(false);
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

  const handleAddToQueue = async () => {
    if (recommendedTracks.length === 0) {
      alert("추천된 곡이 없습니다.");
      return;
    }

    const trackUris = recommendedTracks.map(track => `spotify:track:${track.trackId}`);
    const result = await spotifyPlayerService.addTracksToQueue(trackUris);

    if (result.success) {
      if (result.playedImmediately) {
        alert("추천 곡 재생을 시작합니다!");
      } else if (result.preservedContext) {
        let message = `추천 곡 ${trackUris.length}개가 큐에 추가되었습니다! 현재 듣고 있는 음악 이후에 재생되고, 그 다음 원래 플레이리스트가 이어집니다.`;
        
        if (result.shuffleEnabled) {
          message += "\n\n⚠️ 셔플 모드가 활성화되어 있어 추천 곡들의 순서가 바뀔 수 있습니다.";
        }
        
        alert(message);
      } else {
        alert("추천 곡들이 재생목록의 끝에 추가되었습니다!");
      }
    } else {
      alert("재생목록 추가에 실패했습니다. Spotify Premium 계정이 활성화되어 있는지 확인해주세요.");
    }
  };

  useEffect(() => {
    const fetchLikedSongs = async () => {
      try {
        const response = await axios.get('/api/liked-songs/ids', { withCredentials: true });
        setLikedTrackIds(new Set(response.data));
      } catch (error) {
        console.error("좋아요 목록 조회 실패:", error);
      }
    };
    fetchLikedSongs();
  }, []);

  const handleToggleLike = async (track) => {
    const isLiked = likedTrackIds.has(track.trackId);
    
    // Optimistic UI Update
    const newLikedTrackIds = new Set(likedTrackIds);
    if (isLiked) {
      newLikedTrackIds.delete(track.trackId);
    } else {
      newLikedTrackIds.add(track.trackId);
    }
    setLikedTrackIds(newLikedTrackIds);

    try {
      const apiEndpoint = isLiked ? '/api/liked-songs/remove' : '/api/liked-songs/add';
      await axios.post(apiEndpoint, 
        { 
          trackId: track.trackId,
          title: track.title,
          artist: track.artist,
          musicUrl: track.spotifyUrl
        }, 
        { withCredentials: true }
      );
    } catch (error) {
      console.error("좋아요 처리 실패:", error);
      alert("요청 처리 중 오류가 발생했습니다.");
      
      // Revert UI on failure
      setLikedTrackIds(likedTrackIds);
    }
  };

  const handleCancelEdit = () => {
    if (onCancel) {
      onCancel();
    } else {
      window.history.back();
    }
  };

  return (
    <div>
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
                        {/* Spotify 로그인 여부와 무관하게 버튼 항상 표시 */}
                          <div className="spotify-link">
                              <button
                                onClick={handleAddToQueue}
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
                                  추천 곡 모두 담기
                                  <img src="/spotify-logo.svg" alt="Spotify" style={{width: '40px', marginLeft: '8px'}}/>
                              </button>
                          </div>
                        {/* ) */}
                    </div>
                    <div className="playlist-body">
                        {recommendedTracks.map((track, index) => (
                            <div key={track.trackId || index} className="playlist-track" style={{display: 'flex', alignItems: 'center', marginBottom: '10px', gap: '10px'}}>
                                <span style={{fontWeight: 'bold', fontSize: '1.1rem', minWidth: '20px', textAlign: 'center'}}>{index + 1}</span>
                                <div style={{flexGrow: 1}}>
                                  <iframe
                                      src={`https://open.spotify.com/embed/track/${track.trackId}?utm_source=generator&theme=0&controls=0`}
                                      width="100%"
                                      height="80"
                                      frameBorder="0"
                                      allowFullScreen=""
                                      allow="autoplay; clipboard-write; encrypted-media; fullscreen; picture-in-picture"
                                      loading="lazy"
                                      title={`${track.title} by ${track.artist}`}
                                  ></iframe>
                                </div>
                                <button 
                                  onClick={() => handleToggleLike(track)} 
                                  className={`like-button ${likedTrackIds.has(track.trackId) ? 'liked' : ''}`}
                                  title={likedTrackIds.has(track.trackId) ? '좋아요 취소' : '좋아요'}
                                >
                                  <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                    <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
                                  </svg>
                                </button>
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