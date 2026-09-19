import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

export default defineConfig({
  plugins: [uni()],
  server: {
    port: 5173,
    proxy: {
      // H5 开发期把 /admin-api 代理到本地后端，避免跨域
      '/admin-api': {
        target: 'http://localhost:48080',
        changeOrigin: true
      }
    }
  }
})
