/**
 * `qrcode` 的最小类型声明。
 *
 * 为什么不装 `@types/qrcode`：它会带进 `@types/node`，而 pnpm 的 peer 解析会把 `vite` 的
 * peer 后缀从 `vite@5.2.8(sass…)(terser…)` 改成带 `(@types/node@…)` 的形态 —— 整份 lockfile
 * 会被连带重写几百行，与本包的改动无关。这里只声明用到的 `toDataURL`，够用、也不引入噪声。
 */
declare module 'qrcode' {
  export interface QRCodeToDataURLOptions {
    /** 生成图片的边长（像素） */
    width?: number
    /** 白边宽度（模块数） */
    margin?: number
    /** 纠错级别 */
    errorCorrectionLevel?: 'L' | 'M' | 'Q' | 'H'
  }

  /** 把文本编成二维码 PNG 的 data URL（浏览器环境走 canvas） */
  export function toDataURL(text: string, options?: QRCodeToDataURLOptions): Promise<string>
}
