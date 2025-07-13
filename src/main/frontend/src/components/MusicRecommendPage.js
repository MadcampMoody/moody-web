import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import './MusicRecommendPage.css';

const MusicRecommendPage = () => {
  const navigate = useNavigate();
  const [inputText, setInputText] = useState('');
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);

  const handleGenerateWords = async () => {
    if (!inputText.trim()) {
      alert('텍스트를 입력해주세요!');
      return;
    }
    
    setLoading(true);
    try {
      const response = await fetch('http://localhost:8080/api/groq/recommend-music', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        credentials: 'include', // 인증 정보를 포함하도록 설정
        body: JSON.stringify({ prompt: inputText }),
      });
      
      const data = await response.json();
      setResult(data);
    } catch (error) {
      console.error('Error:', error);
      setResult({ keywords: '', tracks: [] });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="music-recommend-page">
      <header className="page-header">
        <button onClick={() => navigate('/')} className="back-button">
          ← 홈으로
        </button>
        <h1>🎵 음악 추천 도우미</h1>
        <p>텍스트를 입력하면 AI가 감정을 분석하여 음악을 추천해드립니다</p>
      </header>

      <div className="content-container">
        <div className="input-section">
          <h2>📝 텍스트 입력</h2>
          <textarea
            value={inputText}
            onChange={(e) => setInputText(e.target.value)}
            placeholder="분석하고 싶은 텍스트를 입력하세요... (일기, 생각, 감정 등)"
            className="text-input"
            rows="10"
          />
          <button 
            onClick={handleGenerateWords} 
            disabled={loading || !inputText.trim()}
            className="generate-button"
          >
            {loading ? '분석 중...' : '🔍 음악 추천'}
          </button>
        </div>

        {result && (
          <div className="result-section">
            <h2>🎯 AI 분석 결과</h2>
            {result.analysis && (
              <div className="analysis-content">
                <div className="analysis-pills">
                  <div className="pills-group">
                    <h3 className="pills-title">장르</h3>
                    {result.analysis.genres.map((genre, index) => (
                      <span key={index} className="pill genre-pill">{genre}</span>
                    ))}
                  </div>
                  <div className="pills-group">
                    <h3 className="pills-title">키워드</h3>
                    {result.analysis.keywords.map((keyword, index) => (
                      <span key={index} className="pill keyword-pill">{keyword}</span>
                    ))}
                  </div>
                </div>
              </div>
            )}
            
            {result.tracks && result.tracks.length > 0 && (
              <div className="music-section">
                <div className="playlist-header">
                  <h2>🎵 {new Date().toLocaleDateString('ko-KR', { 
                    year: 'numeric', 
                    month: 'long', 
                    day: 'numeric' 
                  })} 추천 플레이리스트</h2>
                  <p className="playlist-subtitle">AI가 당신의 감정을 분석하여 만든 특별한 플레이리스트</p>
                </div>
                <div className="playlist-container">
                  <div className="playlist-info">
                    <div className="playlist-cover">
                      <div className="playlist-icon">🎶</div>
                    </div>
                    <div className="playlist-details">
                      <h3 className="playlist-title">
                        {new Date().toLocaleDateString('ko-KR').replace(/\./g, '-')} 플레이리스트
                      </h3>
                      <p className="playlist-description">총 {result.tracks.length}곡</p>
                    </div>
                  </div>
                  <div className="track-list">
                    {result.tracks.map((track, index) => (
                      <div key={index} className="track-item">
                        <div className="track-number">{index + 1}</div>
                        <div className="track-embed">
                          <iframe
                            src={`https://open.spotify.com/embed/track/${track.trackId}?utm_source=generator&theme=0`}
                            width="100%"
                            height="80"
                            frameBorder="0"
                            allowfullscreen=""
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
        )}
      </div>
    </div>
  );
};

export default MusicRecommendPage; 