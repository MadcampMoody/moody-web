import {useEffect, useState} from "react";
import axios from "axios";
import { Link } from "react-router-dom";

function Home() {
  const [hello, setHello] = useState('');

  useEffect(() => {
    axios.get('/api/test')
        .then((res) => {
          setHello(res.data);
        })
  }, []);

  return (
      <div className="App" style={{ padding: '20px', fontFamily: 'Arial, sans-serif' }}>
        <h1>🎵 Moody Music App</h1>
        <p>백엔드 데이터: {hello}</p>
        
        <div style={{ marginTop: '30px' }}>
          <h2>🎶 페이지 이동</h2>
          <nav style={{ display: 'flex', gap: '15px', marginTop: '20px', flexWrap: 'wrap' }}>
            <Link 
              to="/spotify" 
              style={{ 
                padding: '10px 20px', 
                backgroundColor: '#1DB954', 
                color: 'white', 
                textDecoration: 'none', 
                borderRadius: '25px',
                fontSize: '16px',
                fontWeight: 'bold'
              }}
            >
              📀 Spotify 플레이리스트 보기
            </Link>
            <Link 
              to="/music_recommend" 
              style={{ 
                padding: '10px 20px', 
                backgroundColor: '#667eea', 
                color: 'white', 
                textDecoration: 'none', 
                borderRadius: '25px',
                fontSize: '16px',
                fontWeight: 'bold'
              }}
            >
              🎵 음악 추천 도우미
            </Link>
          </nav>
        </div>

        <div style={{ marginTop: '30px' }}>
          <h2>🔗 API 테스트</h2>
          <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
            <a 
              href="/api/playlists" 
              target="_blank" 
              rel="noopener noreferrer"
              style={{ 
                padding: '8px 15px', 
                backgroundColor: '#007bff', 
                color: 'white', 
                textDecoration: 'none', 
                borderRadius: '5px' 
              }}
            >
              Playlists API
            </a>
            <a 
              href="/api/music" 
              target="_blank" 
              rel="noopener noreferrer"
              style={{ 
                padding: '8px 15px', 
                backgroundColor: '#28a745', 
                color: 'white', 
                textDecoration: 'none', 
                borderRadius: '5px' 
              }}
            >
              Music API
            </a>
          </div>
        </div>
      </div>
  );
}

export default Home; 