import React, { useState, useEffect } from 'react';
import './MusicRecommendPage.css';

const MusicRecommendPage = () => {
    const [recommendedTracks, setRecommendedTracks] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [userName, setUserName] = useState('사용자'); // 기본값 설정
    const [diaryText, setDiaryText] = useState(''); // 일기 내용을 위한 상태 추가

    // 사용자 정보 가져오기
    useEffect(() => {
        const fetchUserInfo = async () => {
            try {
                const response = await fetch('/api/user/me', {
                    credentials: 'include' // 쿠키 기반 인증
                });
                if (response.ok) {
                    const userData = await response.json();
                    setUserName(userData.name || '사용자');
                }
            } catch (error) {
                console.error('사용자 정보 로딩 실패:', error);
            }
        };
        fetchUserInfo();
    }, []);

    const handleRecommendation = async () => {
        if (!diaryText) {
            setError('일기 내용이 비어있습니다.');
            return;
        }

        setLoading(true);
        setError(null);
        setRecommendedTracks([]);

        try {
            const response = await fetch('/api/groq/recommend-music', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                credentials: 'include', // 쿠키 기반 인증
                body: JSON.stringify({ prompt: diaryText }),
            });

            if (!response.ok) {
                if (response.status === 401) {
                    throw new Error('로그인이 필요합니다. 다시 로그인해주세요.');
                }
                throw new Error(`HTTP ${response.status} 에러 발생`);
            }

            const data = await response.json();
            setRecommendedTracks(data.tracks || []);

        } catch (error) {
            setError(`추천을 받는 중 오류가 발생했습니다: ${error.message}`);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="recommendation-page-container">
            <div className="input-section">
                <h2>📝 오늘 하루를 기록해보세요</h2>
                <textarea
                    value={diaryText}
                    onChange={(e) => setDiaryText(e.target.value)}
                    placeholder="오늘의 감정, 생각, 있었던 일들을 자유롭게 적어보세요. AI가 당신의 글을 분석해 무드에 맞는 플레이리스트를 만들어 드려요."
                    className="text-input"
                    rows="10"
                />
            </div>

            <button onClick={handleRecommendation} disabled={loading || !diaryText.trim()} className="recommend-button">
                {loading ? '나를 위한 무드 추천 중...' : '나를 위한 무드 추천받기'}
            </button>
            
            {error && <p className="error-message">{error}</p>}

            {recommendedTracks.length > 0 && (
                <div className="playlist-container">
                    <h2 className="playlist-heading">{`${userName}님의 현재 무드에 맞는 플레이리스트를 만들어봤어요!`}</h2>
                    <div className="playlist-card">
                        <div className="playlist-header">
                            <div className="header-icon">음악 앨범</div>
                            <div className="header-title">플리제목</div>
                            <div className="spotify-link">
                                <a href="https://open.spotify.com" target="_blank" rel="noopener noreferrer">
                                    spotify에서 듣기
                                    <img src="https://storage.googleapis.com/pr-newsroom-wp/1/2018/11/Spotify_Logo_CMYK_Green.png" alt="Spotify" />
                                </a>
                            </div>
                        </div>
                        <div className="playlist-body">
                            {recommendedTracks.map((track, index) => (
                                <div key={track.trackId || index} className="playlist-track">
                                    <span className="track-number">{index + 1}</span>
                                    <div className="track-player-wrapper">
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
};

export default MusicRecommendPage; 