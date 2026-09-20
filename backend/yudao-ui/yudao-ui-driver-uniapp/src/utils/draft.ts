import { uploadImage } from '@/utils/upload'
import { AbnormalReportReq, NodeReportReq, reportAbnormal, reportNode } from '@/api/task'

/** 尚未上传的照片（base64），补传时先上传拿到 URL */
export interface DraftPhoto {
  key: string
  dataUrl: string
}

/**
 * 弱网时暂存在本地的一次上报。
 *
 * <p>节点与异常共用一份草稿队列：异常（道路封闭、没信号）恰恰更容易发生在弱网时。
 * 旧草稿没有 `kind` 字段，按 NODE 兼容。
 */
export interface NodeDraft {
  clientRequestId: string
  createdAt: number
  summary: string
  /** 上报类型：NODE-运输节点，ABNORMAL-运输异常；空按 NODE 兼容旧草稿 */
  kind?: 'NODE' | 'ABNORMAL'
  payload: Omit<NodeReportReq, 'photos'> | Omit<AbnormalReportReq, 'photos'>
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
      // 幂等靠服务端的 `clientRequestId`：同一请求号重复提交只会落一条，
      // 所以这里可以放心重试，不需要本地记录「传到哪一条了」。
      if (draft.kind === 'ABNORMAL') {
        await reportAbnormal({ ...draft.payload, photos } as AbnormalReportReq)
      } else {
        await reportNode({ ...draft.payload, photos } as NodeReportReq)
      }
      removeDraft(draft.clientRequestId)
      success++
    } catch (e) {
      failed++
      updateDraftError(draft.clientRequestId, (e as Error).message)
    }
  }
  return { success, failed }
}
