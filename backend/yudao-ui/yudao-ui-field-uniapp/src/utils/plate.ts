/** 车牌比对：去掉空白并大写后比较；任一为空返回 null（无法比对） */
export function comparePlate(a?: string | null, b?: string | null): boolean | null {
  if (!a || !b) {
    return null
  }
  return normalize(a) === normalize(b)
}

function normalize(plate: string): string {
  return plate.replace(/\s/g, '').toUpperCase()
}
