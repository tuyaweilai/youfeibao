import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

/**
 * 共享包 `@youfeibao/field-shared`（自然人准入四步的 API 与业务逻辑）**直接吃源码，没有构建产物**。
 *
 * 它内部的 `@/utils/request` 由**本工程**的别名解析——请求封装与登录态不共享：
 * 两端的存储键前缀不同（`field_` / `driver_`），恰恰是不该共享的部分。
 */
export default defineConfig({
  plugins: [uni()],
  resolve: {
    alias: [
      // 注意：Vite 的字符串别名是**前缀替换**，所以这里区分「精确命中包名」与「子路径」两种，
      // 否则 `@youfeibao/field-shared/src/api/x` 会被拼成 `.../index.ts/src/api/x`。
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
