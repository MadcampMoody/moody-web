import React from 'react';
import { Link } from 'react-router-dom';

function SpotifyPage() {
  const playlistId = '28AYmcGUYtYgzGJQ4dY1ce';
  const embedUrl = `https://open.spotify.com/embed/playlist/${playlistId}?utm_source=generator&theme=0`;

  return (
    <div style={{ 
      padding: '20px', 
      fontFamily: 'Arial, sans-serif',
      maxWidth: '1200px',
      margin: '0 auto'
    }}>
      {/* Navigation */}
      <nav style={{ marginBottom: '30px' }}>
        <Link 
          to="/" 
          style={{ 
            padding: '10px 20px', 
            backgroundColor: '#333', 
            color: 'white', 
            textDecoration: 'none', 
            borderRadius: '5px',
            fontSize: '14px'
          }}
        >
          ← 홈으로 돌아가기
        </Link>
      </nav>

      {/* Header */}
      <header style={{ textAlign: 'center', marginBottom: '30px' }}>
        <h1 style={{ 
          color: '#1DB954', 
          marginBottom: '10px',
          fontSize: '2.5rem'
        }}>
          🎵 Discover Playlist
        </h1>
        <p style={{ 
          color: '#666', 
          fontSize: '1.1rem',
          marginBottom: '20px'
        }}>
          제형의 플레이리스트 • 1시간 32분 • 다양한 장르의 음악 모음
        </p>
        
        {/* Original Spotify Link */}
        <a 
          href={`https://open.spotify.com/playlist/${playlistId}`}
          target="_blank"
          rel="noopener noreferrer"
          style={{
            display: 'inline-block',
            padding: '12px 24px',
            backgroundColor: '#1DB954',
            color: 'white',
            textDecoration: 'none',
            borderRadius: '25px',
            fontSize: '16px',
            fontWeight: 'bold',
            marginBottom: '30px'
          }}
        >
          🎧 Spotify에서 열기
        </a>
      </header>

      {/* Spotify Embed */}
      <div style={{ 
        display: 'flex', 
        justifyContent: 'center',
        marginBottom: '30px'
      }}>
        <iframe 
          src={embedUrl}
          width="100%" 
          height="580" 
          frameBorder="0" 
          allowfullscreen="" 
          allow="autoplay; clipboard-write; encrypted-media; fullscreen; picture-in-picture"
          loading="lazy"
          style={{
            borderRadius: '12px',
            maxWidth: '600px',
            boxShadow: '0 4px 20px rgba(0,0,0,0.1)'
          }}
        />
      </div>

      {/* Playlist Info */}
      <div style={{ 
        backgroundColor: '#f8f9fa',
        padding: '20px',
        borderRadius: '10px',
        marginBottom: '20px'
      }}>
        <h3 style={{ marginBottom: '15px', color: '#333' }}>📀 플레이리스트 정보</h3>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))', gap: '15px' }}>
          <div>
            <strong>📅 마지막 업데이트:</strong><br />
            10/07/2025, 16:41:34
          </div>
          <div>
            <strong>⏱️ 총 재생시간:</strong><br />
            1시간 32분
          </div>
          <div>
            <strong>🎭 장르:</strong><br />
            Hip-Hop, R&B, Alternative, Electronic
          </div>
        </div>
      </div>

      {/* Featured Artists */}
      <div style={{ 
        backgroundColor: '#fff',
        padding: '20px',
        borderRadius: '10px',
        border: '1px solid #e9ecef'
      }}>
        <h3 style={{ marginBottom: '15px', color: '#333' }}>🎤 주요 아티스트</h3>
        <div style={{ 
          display: 'flex', 
          flexWrap: 'wrap', 
          gap: '10px' 
        }}>
          {[
            'Meek Mill', 'Bakar', 'Khruangbin', 'Key Glock', 'Larry June',
            'Drake', 'Doja Cat', 'blackbear', 'Aminé', 'André 3000',
            'Denzel Curry', 'Rex Orange County', 'SZA', 'Joey Bada$$'
          ].map((artist, index) => (
            <span 
              key={index}
              style={{
                padding: '5px 12px',
                backgroundColor: '#e9ecef',
                borderRadius: '15px',
                fontSize: '14px',
                color: '#333'
              }}
            >
              {artist}
            </span>
          ))}
        </div>
      </div>
    </div>
  );
}

export default SpotifyPage; 