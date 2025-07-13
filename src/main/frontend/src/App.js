import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Home from './components/Home';
import SpotifyPage from './components/SpotifyPage';
import MusicRecommendPage from './components/MusicRecommendPage';
import './App.css';

function App() {
  return (
    <Router>
      <div className="App">
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/spotify" element={<SpotifyPage />} />
          <Route path="/music_recommend" element={<MusicRecommendPage />} />
        </Routes>
      </div>
    </Router>
  );
}

export default App;