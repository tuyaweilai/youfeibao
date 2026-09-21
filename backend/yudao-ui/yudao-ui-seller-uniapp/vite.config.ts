import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

export default defineConfig({
  plugins: [uni()],
  // 共享包 `@youfeibao/field-shared`：自然人端只引它的**类型**（`import type`，运行时会被擦除），
  // 这样不会把现场端 / 司机端的请求层与登录态带过来（见 packages/field-shared/src/api/wizardTypes.ts）。
  resolve: {
    alias: [
      // 前缀替换：精确命中包名与子路径分开写，否则 `.../src/api/x` 会被拼成 `.../index.ts/src/api/x`
      {
        find: /^@youfeibao\/field-shared$/,
        replacement: new URL('../packages/field-shared/src/index.ts', import.meta.url).pathname
      },
      {
        find: /^@youfeibao\/field-shared\//,
        replacement: new URL('../packages/field-shared/', import.meta.url).pathname
      }
    ]
  },
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
