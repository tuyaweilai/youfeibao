/**
 * 拍照 / 选图并读成 dataURL（本人自填建档用，#94）。
 *
 * 与现场端 `upload.ts` 的差别：本人端一期是**微信小程序优先**，所以 mp-weixin 分支要真的
 * `getFileSystemManager().readFile(base64)` 把临时文件读成 base64——临时路径不能直接当识别入参。
 * 这里只保留「读图」，不上传：识别是无状态的，图片识别完即弃（ADR 0037）。
 */

/** 拍照 / 从相册选图，返回本地临时路径 */
export function chooseImage(count = 1): Promise<string[]> {
  return new Promise<string[]>((resolve, reject) => {
    uni.chooseImage({
      count,
      sizeType: ['compressed'],
      sourceType: ['camera', 'album'],
      success: (res) => resolve(res.tempFilePaths as string[]),
      fail: (err) => reject(new Error(err.errMsg || '选择照片失败'))
    })
  })
}

/** 把本地图片路径读成 base64 dataURL（H5 用 FileReader，小程序用 readFile） */
export function pathToDataUrl(tempPath: string): Promise<string> {
  // #ifdef H5
  return (async () => {
    const blob = await (await fetch(tempPath)).blob()
    return await new Promise<string>((resolve, reject) => {
      const reader = new FileReader()
      reader.onload = () => resolve(reader.result as string)
      reader.onerror = () => reject(new Error('读取照片失败'))
      reader.readAsDataURL(blob)
    })
  })()
  // #endif
  // #ifndef H5
  return new Promise<string>((resolve, reject) => {
    const mime = tempPath.toLowerCase().endsWith('.png') ? 'image/png' : 'image/jpeg'
    uni.getFileSystemManager().readFile({
      filePath: tempPath,
      encoding: 'base64',
      success: (res) => resolve(`data:${mime};base64,${res.data as string}`),
      fail: (err) => reject(new Error(err.errMsg || '读取照片失败'))
    })
  })
  // #endif
}

/** 去掉 dataURL 前缀：识别接口要的是纯 base64（图片随请求进来、识别完即弃） */
export function toBase64(dataUrl: string): string {
  const index = dataUrl.indexOf(',')
  return index >= 0 ? dataUrl.slice(index + 1) : dataUrl
}
