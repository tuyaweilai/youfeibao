import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

export default defineConfig({
  plugins: [uni()],
  // `qrcode` 是 CommonJS；它依赖的 `dijkstrajs` 已显式写进 dependencies（pnpm 不提升幽灵依赖，
  // 否则 Rollup 会留下裸外部依赖，浏览器 404）。与现场端同一做法。
  optimizeDeps: {
    include: ['qrcode']
  },
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
