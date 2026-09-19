import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

export default defineConfig({
  plugins: [uni()],
  server: {
    port: 5174,
    proxy: {
      // H5 开发期把公开端点与自然人端登录态通道代理到本地后端
      '/admin-api': {
        target: 'http://localhost:48080',
        changeOrigin: true
      },
      '/app-api': {
        target: 'http://localhost:48080',
        changeOrigin: true
      }
    }
  }
})
