import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

export default defineConfig({
  plugins: [uni()],
  server: {
    // 收货员现场端占 5173，司机端用 5174：两个端可能同时在开发机跑
    port: 5174,
    proxy: {
      // H5 开发期把 /admin-api 代理到本地后端，避免跨域
      '/admin-api': {
        target: 'http://localhost:48080',
        changeOrigin: true
      }
    }
  }
})
