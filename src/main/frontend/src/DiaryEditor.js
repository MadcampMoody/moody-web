import axios from "axios";
import React, { useState, useEffect, useRef } from "react";
import "./DiaryEditor.css";

function DiaryEditor({ selectedDate, selectedMood, initialContent = "", onCancel }) {
  const [content, setContent] = useState(initialContent);

  // 음악 추천 관련 state
  const [recommendedTracks, setRecommendedTracks] = useState([]);
  const [playlistTitle, setPlaylistTitle] = useState('');
  const [loading, setLoading] = useState(false);
  const [initialLoading, setInitialLoading] = useState(true);
  const [error, setError] = useState(null);
  const [userName, setUserName] = useState('사용자');
  const playlistRef = useRef(null);

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


  // 날짜 포맷 함수
  const formatDate = (dateString) => {
    if (!dateString) return "";
    const [year, month, day] = dateString.split("-");
    return `${year}년 ${parseInt(month)}월 ${parseInt(day)}일`;
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

  return (
    <div style={{ padding: '20px', maxWidth: '800px', margin: '0 auto' }}>
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
          <button className="diary-save-btn" onClick={handleSave} disabled={loading}>
            {loading ? '추천받는 중...' : '저장 및 음악 추천받기'}
          </button>
          <button className="diary-cancel-btn" onClick={onCancel} disabled={loading}>
            취소
          </button>
        </div>
      </div>

      {error && <p className="error-message" style={{color: 'red', marginTop: '20px', textAlign: 'center'}}>{error}</p>}

      {initialLoading && (
        <div style={{textAlign: 'center', marginTop: '20px', color: '#666'}}>
          기존 플레이리스트 확인 중...
        </div>
      )}

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
  );
}

export default DiaryEditor;