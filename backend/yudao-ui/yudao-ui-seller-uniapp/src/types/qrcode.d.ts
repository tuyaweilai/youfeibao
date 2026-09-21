/**
 * `qrcode` 的最小类型声明。
 *
 * 为什么不装 `@types/qrcode`：它会带进 `@types/node`，pnpm 的 peer 解析会连带重写整份
 * lockfile（vite 的 peer 后缀全变），与本包的改动无关。这里只声明用到的 `toDataURL`。
 * 与现场端 `yudao-ui-field-uniapp/src/types/qrcode.d.ts` 同一做法。
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
