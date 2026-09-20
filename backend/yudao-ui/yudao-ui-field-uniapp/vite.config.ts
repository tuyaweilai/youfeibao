import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

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
