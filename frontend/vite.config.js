import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // 開発中は /api を Spring Boot（8080）へ転送し、画面と API を同じオリジンに見せる。
    // 別オリジンのままだと CORS の設定と、セッションの Cookie の扱いで詰まるため
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
