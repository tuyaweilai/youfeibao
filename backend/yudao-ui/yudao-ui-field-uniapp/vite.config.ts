import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

export default defineConfig({
  plugins: [uni()],
  // `qrcode` 是 CommonJS：预打包一次，dev 下少一截逐文件转换（管理后台同样把它写进 include）。
  // 它依赖的 `dijkstrajs` 还必须显式写进 dependencies：pnpm 不做幽灵依赖提升，否则 Rollup 解析不到，
  // 会把它留成 `import "dijkstrajs"` 的裸外部依赖 —— 浏览器 404、二维码出不来。
  optimizeDeps: {
    include: ['qrcode']
  },
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
