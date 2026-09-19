/** 当前网络类型；取不到时按 unknown 处理 */
export function getNetworkType(): Promise<string> {
  return new Promise<string>((resolve) => {
    uni.getNetworkType({
      success: (res) => resolve(res.networkType),
      fail: () => resolve('unknown')
    })
  })
}

export async function isOnline(): Promise<boolean> {
  const type = await getNetworkType()
  return type !== 'none'
}

/** 监听网络变化，网络恢复时回调 */
export function onNetworkChange(callback: (online: boolean) => void) {
  uni.onNetworkStatusChange((res) => callback(res.isConnected))
}
