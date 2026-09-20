import request from '@/config/axios'

// 运输轨迹**演示件**（#76 V9）：模拟数据，不代表真实行驶路径。
// 后端不落库、不进一票一档与任何报表；这里也只在任务详情里展示，不做任何写入。
export interface TrackPointVO {
  latitude?: number
  longitude?: number
  time?: number
  simulated?: boolean
}

export interface TrackAnchorVO {
  nodeTypeName?: string
  nodeTime?: number
  latitude?: number
  longitude?: number
}

export interface TransportTrackDemoVO {
  enabled?: boolean
  source?: string // 恒为 SIMULATED
  note?: string
  points?: TrackPointVO[]
  anchors?: TrackAnchorVO[]
}

export const LogisticsTrackDemoApi = {
  /** 取某趟任务的演示轨迹（开关关闭时 enabled=false 且不带点） */
  getTrack: async (taskId: number) =>
    await request.get({ url: `/logistics/demo/transport-track`, params: { taskId } })
}
