import axios from 'axios';

const instance = axios.create({
  baseURL: 'http://127.0.0.1:8080', // 백엔드 서버 주소를 127.0.0.1로 변경
  withCredentials: true, // 모든 요청에 쿠키를 포함
});

export default instance; 