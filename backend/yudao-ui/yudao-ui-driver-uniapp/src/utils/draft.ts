import { uploadImage } from '@/utils/upload'
import { NodeReportReq, reportNode } from '@/api/task'

/** 尚未上传的照片（base64），补传时先上传拿到 URL */
export interface DraftPhoto {
  key: string
  dataUrl: string
}

/** 弱网时暂存在本地的一次节点上报 */
export interface NodeDraft {
  clientRequestId: string
  createdAt: number
  summary: string
  payload: Omit<NodeReportReq, 'photos'>
  photos: DraftPhoto[]
  /** 已经上传好的照片 URL */
  photoUrls: Record<string, string>
  errorMsg?: string
}

const DRAFT_KEY = 'driver_node_drafts'

export function listDrafts(): NodeDraft[] {
  return uni.getStorageSync(DRAFT_KEY) || []
}

export function countDrafts(): number {
  return listDrafts().length
}

export function saveDraft(draft: NodeDraft) {
  // 同一个 clientRequestId 只留一份：重复点「上报」不该在本地堆出两条草稿
  const list = listDrafts().filter((item) => item.clientRequestId !== draft.clientRequestId)
  list.unshift(draft)
  persist(list)
}

export function removeDraft(clientRequestId: string) {
  persist(listDrafts().filter((item) => item.clientRequestId !== clientRequestId))
}

function updateDraftError(clientRequestId: string, errorMsg: string) {
  const list = listDrafts()
  const draft = list.find((item) => item.clientRequestId === clientRequestId)
  if (draft) {
    draft.errorMsg = errorMsg
    persist(list)
  }
}

function persist(list: NodeDraft[]) {
  uni.setStorageSync(DRAFT_KEY, list)
}

/**
 * 补传全部草稿：照片先上传拿 URL，再提交节点。
 *
 * <p>幂等靠服务端的 `clientRequestId`：同一请求号重复提交只会落一条节点，
 * 所以这里可以放心重试，不需要本地记录「传到哪一条了」。
 *
 * @returns 成功与失败的条数
 */
export async function syncOfflineNodes(): Promise<{ success: number; failed: number }> {
  let success = 0
  let failed = 0
  for (const draft of listDrafts()) {
    try {
      const photos: string[] = []
      for (const photo of draft.photos) {
        const url = draft.photoUrls[photo.key] || (await uploadImage(photo.dataUrl))
        draft.photoUrls[photo.key] = url
        photos.push(url)
      }
      await reportNode({ ...draft.payload, photos })
      removeDraft(draft.clientRequestId)
      success++
    } catch (e) {
      failed++
      updateDraftError(draft.clientRequestId, (e as Error).message)
    }
  }
  return { success, failed }
}
