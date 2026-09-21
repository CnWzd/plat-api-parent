import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    proxy: {
      // 管理后台 API：统一走网关（JWT 鉴权前置）
      '/api': { target: 'http://127.0.0.1:8080', changeOrigin: true },
      // 开放 API 在线调试：走网关签名校验链路
      '/openapi': { target: 'http://127.0.0.1:8080', changeOrigin: true },
    },
  },
  build: {
    chunkSizeWarningLimit: 1500,
  },
})
