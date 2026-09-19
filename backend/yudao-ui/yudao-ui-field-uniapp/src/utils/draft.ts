import { AcquisitionCreateReq, syncOfflineAcquisitions } from '@/api/acquisition'
import { dataUrlToUploadPath, uploadImage } from '@/utils/upload'

/** 尚未上传的照片（base64），补传时先上传拿到 URL */
export interface DraftPhoto {
  key: string
  dataUrl: string
}

/** 弱网时暂存在本地的一笔收购登记 */
export interface AcquisitionDraft {
  clientRequestId: string
  createdAt: number
  summary: string
  payload: AcquisitionCreateReq
  photos: DraftPhoto[]
  /** 已经上传好的照片 URL */
  photoUrls: Record<string, string>
  errorMsg?: string
}

const DRAFT_KEY = 'field_acquisition_drafts'

export function listDrafts(): AcquisitionDraft[] {
  return uni.getStorageSync(DRAFT_KEY) || []
}

export function countDrafts(): number {
  return listDrafts().length
}

export function saveDraft(draft: AcquisitionDraft) {
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

function persist(list: AcquisitionDraft[]) {
  try {
    uni.setStorageSync(DRAFT_KEY, list)
  } catch {
    throw new Error('本地暂存失败：照片过多或存储空间不足，请先补传已有草稿')
  }
}

/**
 * 逐条补传：先把草稿里的照片传上去，再走 `/icbc/acquisition/sync-offline`（按 clientRequestId 幂等）。
 * 成功即删草稿；失败留在本地并记下原因，可单独重试。
 */
export async function syncDrafts(clientRequestIds?: string[]): Promise<{ success: number; failed: number }> {
  const drafts = clientRequestIds
    ? listDrafts().filter((item) => clientRequestIds.includes(item.clientRequestId))
    : listDrafts()
  let success = 0
  let failed = 0
  for (const draft of drafts) {
    try {
      const photoUrls: Record<string, string> = { ...draft.photoUrls }
      for (const photo of draft.photos) {
        if (!photoUrls[photo.key]) {
          photoUrls[photo.key] = await uploadImage(dataUrlToUploadPath(photo.dataUrl))
        }
      }
      const results = await syncOfflineAcquisitions([{ ...draft.payload, ...photoUrls }])
      const result = results.find((item) => item.clientRequestId === draft.clientRequestId) ?? results[0]
      if (result?.success) {
        removeDraft(draft.clientRequestId)
        success++
      } else {
        updateDraftError(draft.clientRequestId, result?.errorMsg || '补传失败')
        failed++
      }
    } catch (e) {
      updateDraftError(draft.clientRequestId, (e as Error).message || '补传失败')
      failed++
    }
  }
  return { success, failed }
}
