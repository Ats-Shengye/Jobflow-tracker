import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [react()],
  server: {
    // 開発中は /api を Spring Boot へ転送し、画面と API を同じオリジンに見せる（CORS と Cookie で詰まらないように）。
    // バックエンドは 127.0.0.1 だけで待ち受けるので、localhost（::1 に解決されうる）ではなく IP で指定する
    proxy: {
      '/api': 'http://127.0.0.1:8080',
    },
  },
})
