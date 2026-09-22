/**
 * 高德地图 JS API 2.0 懒加载。
 *
 * 只在真正需要看地图时才注入 script：管理后台大部分页面用不到地图，
 * 没必要每一次登录都去拉一个几百 KB 的第三方脚本。
 *
 * key 走 VITE_AMAP_KEY（前端 key 本身就是公开的，放进页面即可；真正的保护在控制台侧配置域名白名单）。
 * 若在高德控制台给 key 配了「安全密钥」，再加 VITE_AMAP_SECURITY_CODE；JSAPI 2.0 会在部分插件上要求它。
 */

/** 仅声明本仓用到的那几个 API，够用就行；高德完整类型包体积太大 */
export interface AMapOverlay {
  setPosition(position: [number, number]): void
  setAngle?(angle: number): void
}

export interface AMapMapInstance {
  add(overlay: AMapOverlay | AMapOverlay[]): void
  setFitView(
    overlays?: AMapOverlay[] | null,
    immediately?: boolean,
    avoid?: number[],
    maxZoom?: number
  ): void
  destroy(): void
}

export interface AMapNS {
  Map: new (container: HTMLElement | string, opts?: Record<string, unknown>) => AMapMapInstance
  Polyline: new (opts: Record<string, unknown>) => AMapOverlay
  Marker: new (opts: Record<string, unknown>) => AMapOverlay
  Pixel: new (x: number, y: number) => unknown
}

declare global {
  interface Window {
    AMap?: AMapNS
    _AMapSecurityConfig?: { securityJsCode: string }
  }
}

const AMAP_KEY = import.meta.env.VITE_AMAP_KEY || 'b48eaeb60945d7428849a5e70f219dff'
const AMAP_SECURITY_CODE = import.meta.env.VITE_AMAP_SECURITY_CODE || ''

/** 同一个页面上多处调用只加载一次 */
let pending: Promise<AMapNS> | undefined

export function loadAMap(): Promise<AMapNS> {
  if (window.AMap) {
    return Promise.resolve(window.AMap)
  }
  if (pending) {
    return pending
  }
  pending = new Promise<AMapNS>((resolve, reject) => {
    if (AMAP_SECURITY_CODE) {
      window._AMapSecurityConfig = { securityJsCode: AMAP_SECURITY_CODE }
    }
    const script = document.createElement('script')
    script.async = true
    script.src = `https://webapi.amap.com/maps?v=2.0&key=${AMAP_KEY}`
    script.onload = () => {
      window.AMap ? resolve(window.AMap) : reject(new Error('高德地图脚本已加载，但 AMap 未初始化'))
    }
    script.onerror = () => {
      // 失败后允许重试（例如断网恢复后重新打开弹窗）
      pending = undefined
      reject(new Error('高德地图加载失败，请检查网络或 VITE_AMAP_KEY 配置'))
    }
    document.head.appendChild(script)
  })
  return pending
}
