<template>
  <ContentWrap>
    <el-alert
      type="info"
      :closable="false"
      class="mb-10px"
      title="结算单：一次到场批次一次确认。确认是开票（预下单）的硬前置；未确认不得发起开票。确认表达的是出售者对计量与计价事实的认可，允许分批开票付款。"
    />
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="100px">
      <el-form-item label="结算单号" prop="settlementNo">
        <el-input v-model="queryParams.settlementNo" placeholder="请输入" clearable @keyup.enter="handleQuery" class="!w-200px" />
      </el-form-item>
      <el-form-item label="出售者" prop="sellerName">
        <el-input v-model="queryParams.sellerName" placeholder="请输入" clearable @keyup.enter="handleQuery" class="!w-180px" />
      </el-form-item>
      <el-form-item label="确认状态" prop="confirmStatus">
        <el-select v-model="queryParams.confirmStatus" placeholder="请选择" clearable class="!w-180px">
          <el-option v-for="item in SETTLEMENT_CONFIRM_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" @click="openGenerate" v-hasPermi="['icbc:settlement-confirm:manage']">
          <Icon icon="ep:plus" class="mr-5px" /> 结束本次收货
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true">
      <el-table-column label="结算单号" prop="settlementNo" min-width="180" />
      <el-table-column label="出售者" prop="sellerName" min-width="110" />
      <el-table-column label="收购单" prop="acquisitionCount" width="90" align="center" />
      <el-table-column label="合计金额" prop="totalAmount" width="130" align="right" />
      <el-table-column label="确认状态" align="center" width="150">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.confirmStatus)">{{ row.confirmStatusName }}</el-tag>
          <el-tag v-if="row.enterpriseNotReplied" type="danger" class="ml-5px">企业未回复</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="异议" align="center" width="120">
        <template #default="{ row }">
          <span v-if="row.disputeReasonName">{{ row.disputeReasonName }}（{{ row.disputeCount }}）</span>
          <span v-else class="text-gray-400">-</span>
        </template>
      </el-table-column>
      <el-table-column label="版本" prop="currentVersionNo" width="80" align="center" />
      <el-table-column label="结清" align="center" width="80">
        <template #default="{ row }">
          <el-tag :type="row.settled ? 'success' : 'info'">{{ row.settled ? '已结清' : '未结清' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="生成时间" prop="generateTime" width="170" :formatter="dateFormatter" />
      <el-table-column label="操作" align="center" width="280" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">明细</el-button>
          <el-button
            v-if="row.confirmStatus === 2"
            link
            type="warning"
            @click="openReply(row)"
            v-hasPermi="['icbc:settlement-confirm:manage']"
          >
            处理异议
          </el-button>
          <el-button
            v-if="row.confirmStatus === 3"
            link
            type="primary"
            @click="openOffline(row)"
            v-hasPermi="['icbc:settlement-confirm:manage']"
          >
            线下签字
          </el-button>
          <el-button
            link
            type="warning"
            @click="openForward(row)"
            v-hasPermi="['icbc:seller-notify:manage']"
          >
            转达确认链接
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </ContentWrap>

  <!-- 生成结算单 -->
  <el-dialog v-model="generateVisible" title="结束本次收货" width="480px">
    <el-alert type="warning" :closable="false" class="mb-10px"
      title="生成后不得再往这张结算单里加收购单，要加只能新建。系统的时间窗只作建议，现场动作才是批次边界。" />
    <el-form :model="generateForm" label-width="110px">
      <el-form-item label="收方档案编号">
        <el-input v-model.number="generateForm.payeeId" placeholder="收方档案 id" />
      </el-form-item>
      <el-form-item label="离线批次键">
        <el-input v-model="generateForm.batchKey" placeholder="选填；现场端同一批用同一个值" />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="generateForm.remark" placeholder="选填" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="generateVisible = false">取消</el-button>
      <el-button type="primary" @click="submitGenerate">生成</el-button>
    </template>
  </el-dialog>

  <!-- 明细 -->
  <el-dialog v-model="detailVisible" title="结算单明细" width="900px">
    <template v-if="detail">
      <el-descriptions :column="3" border class="mb-10px">
        <el-descriptions-item label="结算单号">{{ detail.settlementNo }}</el-descriptions-item>
        <el-descriptions-item label="出售者">{{ detail.sellerName }}</el-descriptions-item>
        <el-descriptions-item label="确认状态">{{ detail.confirmStatusName }}</el-descriptions-item>
        <el-descriptions-item label="确认时间">{{ detail.confirmTime ? formatDate(detail.confirmTime) : '-' }}</el-descriptions-item>
        <el-descriptions-item label="快照哈希" :span="2">{{ detail.confirmHash || '-' }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.offlineSignHandler" label="线下办理人">
          {{ detail.offlineSignHandler }}（{{ detail.offlineSignTime ? formatDate(detail.offlineSignTime) : '' }}）
        </el-descriptions-item>
        <el-descriptions-item v-if="detail.enterpriseReplyNote" label="企业说明" :span="2">
          {{ detail.enterpriseReplyNote }}
        </el-descriptions-item>
      </el-descriptions>
      <el-table :data="detail.lines" :stripe="true" size="small">
        <el-table-column label="收购单号" prop="acquisitionNo" min-width="150" />
        <el-table-column label="品类" prop="categoryName" min-width="90" />
        <el-table-column label="结算重量" prop="settlementWeight" width="110" align="right" />
        <el-table-column label="单价" prop="unitPrice" width="100" align="right" />
        <el-table-column label="调整项" prop="adjustmentAmount" width="100" align="right" />
        <el-table-column label="金额" prop="amount" width="110" align="right" />
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.status === 9" type="danger">已作废</el-tag>
            <span v-else>{{ row.statusName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" align="center">
          <template #default="{ row }">
            <el-button
              v-if="row.status !== 9"
              link
              type="danger"
              @click="openCancel(row)"
              v-hasPermi="['icbc:settlement-confirm:manage']"
            >
              作废
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-divider content-position="left">版本留痕</el-divider>
      <el-table :data="detail.versions" size="small">
        <el-table-column label="版本" prop="versionNo" width="70" align="center" />
        <el-table-column label="变更原因" prop="changeReason" min-width="180" />
        <el-table-column label="变更人" prop="changedBy" width="100" />
        <el-table-column label="来源" prop="source" width="150" />
        <el-table-column label="快照哈希" prop="snapshotHash" min-width="220" show-overflow-tooltip />
        <el-table-column label="时间" prop="createTime" width="170" :formatter="dateFormatter" />
      </el-table>
    </template>
    <template #footer>
      <el-button @click="detailVisible = false">关闭</el-button>
      <el-button
        v-if="detail && !detail.confirmTime"
        type="warning"
        @click="openForward(detail)"
        v-hasPermi="['icbc:seller-notify:manage']"
      >
        转达确认链接
      </el-button>
    </template>
  </el-dialog>

  <!-- 处理异议：改 / 不改但附说明 -->
  <el-dialog v-model="replyVisible" title="处理异议" width="820px">
    <el-alert type="info" :closable="false" class="mb-10px"
      title="只有两个动作：改（产生新版本 + 原因，回到待确认）或 不改但附说明（也回到待确认）。不做聊天、不做工单。" />
    <el-radio-group v-model="replyMode" class="mb-10px">
      <el-radio label="change">改（新版本）</el-radio>
      <el-radio label="reply">不改但附说明</el-radio>
    </el-radio-group>
    <template v-if="replyMode === 'reply'">
      <el-input v-model="replyNote" type="textarea" :rows="3" placeholder="说明为什么这一项不改（必填）" />
    </template>
    <template v-else>
      <el-input v-model="changeReason" class="mb-10px" placeholder="变更原因（说清改了哪一项，必填）" />
      <el-table :data="changeLines" size="small">
        <el-table-column label="收购单号" prop="acquisitionNo" min-width="150" />
        <el-table-column label="扣杂" width="120">
          <template #default="{ row }"><el-input v-model="row.deduction" placeholder="0" /></template>
        </el-table-column>
        <el-table-column label="扣杂录法" width="130">
          <template #default="{ row }">
            <el-select v-model="row.deductionMethod">
              <el-option label="按重量" value="WEIGHT" />
              <el-option label="按比例" value="RATIO" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="单价" width="120">
          <template #default="{ row }"><el-input v-model="row.unitPrice" placeholder="0.00" /></template>
        </el-table-column>
        <el-table-column label="调整项" width="120">
          <template #default="{ row }"><el-input v-model="row.adjustmentAmount" placeholder="0.00" /></template>
        </el-table-column>
        <el-table-column label="调整原因" min-width="160">
          <template #default="{ row }"><el-input v-model="row.adjustmentReason" placeholder="非 0 时必填" /></template>
        </el-table-column>
      </el-table>
    </template>
    <template #footer>
      <el-button @click="replyVisible = false">取消</el-button>
      <el-button type="primary" @click="submitReply">提交</el-button>
    </template>
  </el-dialog>

  <!-- 线下签字 -->
  <el-dialog v-model="offlineVisible" title="线下签字确认" width="480px">
    <el-alert type="warning" :closable="false" class="mb-10px"
      title="长期不确认时走线下签字逃生门：上传带签字的纸质确认书 + 办理人，等价于确认。" />
    <el-form :model="offlineForm" label-width="120px">
      <el-form-item label="确认书附件地址">
        <el-input v-model="offlineForm.fileUrl" placeholder="带签字的纸质确认书 URL" />
      </el-form-item>
      <el-form-item label="办理人">
        <el-input v-model="offlineForm.handler" placeholder="办理人姓名" />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="offlineForm.remark" placeholder="选填" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="offlineVisible = false">取消</el-button>
      <el-button type="primary" @click="submitOffline">确认</el-button>
    </template>
  </el-dialog>

  <!-- 转达确认链接（#36）：首次交易、从未留手机号的场景只有这条通路 -->
  <el-dialog v-model="forwardVisible" title="把确认链接转达给出售者" width="560px">
    <el-alert type="info" :closable="false" class="mb-10px"
      title="链接打开即可查看（需手机号验证才能确认），收短信的人不需要注册。没留手机号时由收货员当面 / 微信转达。" />
    <el-checkbox v-model="forwardSendSms" :disabled="!forwardResult?.mobileMasked && !forwardSending">
      同时发短信到出售者手机号
    </el-checkbox>
    <el-descriptions v-if="forwardResult" :column="1" border class="mt-10px">
      <el-descriptions-item label="结果">{{ forwardResult.message }}</el-descriptions-item>
      <el-descriptions-item label="手机号">{{ forwardResult.mobileMasked || '未留手机号' }}</el-descriptions-item>
      <el-descriptions-item label="链接">
        <div class="break-all">{{ forwardResult.link || '未配置自然人端入口地址' }}</div>
        <el-button v-if="forwardResult.link" link type="primary" @click="copyForward(forwardResult.link)">复制链接</el-button>
      </el-descriptions-item>
      <el-descriptions-item label="短信文案">
        <div class="break-all">{{ forwardResult.notificationText }}</div>
        <el-button link type="primary" @click="copyForward(forwardResult.notificationText || '')">复制文案</el-button>
      </el-descriptions-item>
    </el-descriptions>
    <template #footer>
      <el-button @click="forwardVisible = false">关闭</el-button>
      <el-button type="primary" :loading="forwardSending" @click="submitForward">
        {{ forwardResult ? '重新生成' : '生成链接' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import {
  SettlementApi,
  SettlementVO,
  SETTLEMENT_CONFIRM_STATUS_OPTIONS
} from '@/api/icbc/settlement'
import { SellerNotifyApi, SellerNotifyForwardLinkVO } from '@/api/icbc/sellerNotify'
import { dateFormatter } from '@/utils/formatTime'

defineOptions({ name: 'IcbcSettlement' })

const message = useMessage()

const loading = ref(true)
const list = ref<SettlementVO[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  settlementNo: undefined,
  sellerName: undefined,
  confirmStatus: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await SettlementApi.getSettlementPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}
const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}
const resetQuery = () => {
  queryFormRef.value.resetFields()
  handleQuery()
}

const statusTagType = (status?: number) => {
  if (status === 1 || status === 4) return 'success'
  if (status === 2) return 'danger'
  if (status === 3) return 'warning'
  return 'info'
}

const formatDate = (value?: Date) => (value ? dateFormatter(undefined as any, undefined as any, value) : '-')

// ==================== 生成 ====================
const generateVisible = ref(false)
const generateForm = reactive<{ payeeId?: number; batchKey?: string; remark?: string }>({})
const openGenerate = () => {
  generateForm.payeeId = undefined
  generateForm.batchKey = undefined
  generateForm.remark = undefined
  generateVisible.value = true
}
const submitGenerate = async () => {
  if (!generateForm.payeeId) {
    message.warning('请填写收方档案编号')
    return
  }
  await SettlementApi.generate({
    payeeId: generateForm.payeeId,
    batchKey: generateForm.batchKey || undefined,
    remark: generateForm.remark || undefined
  })
  message.success('结算单已生成')
  generateVisible.value = false
  await getList()
}

// ==================== 明细 ====================
const detailVisible = ref(false)
const detail = ref<SettlementVO>()
const openDetail = async (row: SettlementVO) => {
  detail.value = await SettlementApi.getSettlement(row.id!)
  detailVisible.value = true
}

// ==================== 处理异议 ====================
const replyVisible = ref(false)
const replyMode = ref<'change' | 'reply'>('reply')
const replyNote = ref('')
const changeReason = ref('')
const changeLines = ref<any[]>([])
const current = ref<SettlementVO>()

const openReply = async (row: SettlementVO) => {
  const settlement = await SettlementApi.getSettlement(row.id!)
  current.value = settlement
  replyMode.value = 'reply'
  replyNote.value = ''
  changeReason.value = ''
  changeLines.value = (settlement.lines || [])
    .filter((line) => line.status !== 9)
    .map((line) => ({
      acquisitionId: line.acquisitionId,
      acquisitionNo: line.acquisitionNo,
      deduction: line.deduction,
      deductionMethod: line.deductionMethod || 'WEIGHT',
      unitPrice: line.unitPrice,
      adjustmentAmount: line.adjustmentAmount,
      adjustmentReason: line.adjustmentReason
    }))
  replyVisible.value = true
}
const submitReply = async () => {
  if (!current.value?.id) return
  if (replyMode.value === 'reply') {
    if (!replyNote.value.trim()) {
      message.warning('请填写说明')
      return
    }
    await SettlementApi.reply({ settlementId: current.value.id, note: replyNote.value })
    message.success('已回复，等待自然人再确认')
  } else {
    if (!changeReason.value.trim()) {
      message.warning('请填写变更原因')
      return
    }
    await SettlementApi.change({
      settlementId: current.value.id,
      changeReason: changeReason.value,
      lines: changeLines.value.map((line) => ({
        acquisitionId: line.acquisitionId,
        deduction: line.deduction === '' || line.deduction == null ? undefined : Number(line.deduction),
        deductionMethod: line.deductionMethod,
        unitPrice: line.unitPrice === '' || line.unitPrice == null ? undefined : Number(line.unitPrice),
        adjustmentAmount:
          line.adjustmentAmount === '' || line.adjustmentAmount == null ? undefined : Number(line.adjustmentAmount),
        adjustmentReason: line.adjustmentReason || undefined
      }))
    })
    message.success('已改版，回到待确认')
  }
  replyVisible.value = false
  await getList()
}

// ==================== 线下签字 ====================
const offlineVisible = ref(false)
const offlineForm = reactive<{ fileUrl?: string; handler?: string; remark?: string }>({})
const openOffline = (row: SettlementVO) => {
  current.value = row
  offlineForm.fileUrl = undefined
  offlineForm.handler = undefined
  offlineForm.remark = undefined
  offlineVisible.value = true
}
const submitOffline = async () => {
  if (!current.value?.id || !offlineForm.fileUrl || !offlineForm.handler) {
    message.warning('请填写确认书附件与办理人')
    return
  }
  await SettlementApi.offlineSign({
    settlementId: current.value.id,
    fileUrl: offlineForm.fileUrl,
    handler: offlineForm.handler,
    remark: offlineForm.remark || undefined
  })
  message.success('已按线下签字确认')
  offlineVisible.value = false
  await getList()
}

// ==================== 作废 ====================
const openCancel = async (line: any) => {
  const { value } = await message.prompt('作废原因（对自然人可见）', '作废收购单')
  await SettlementApi.cancelAcquisition({ acquisitionId: line.acquisitionId, reason: value })
  message.success('已作废')
  detail.value = await SettlementApi.getSettlement(detail.value!.id!)
}

// ==================== 转达确认链接（#36） ====================
const forwardVisible = ref(false)
const forwardSending = ref(false)
const forwardSendSms = ref(false)
const forwardTarget = ref<SettlementVO>()
const forwardResult = ref<SellerNotifyForwardLinkVO>()
const openForward = (row: SettlementVO) => {
  forwardTarget.value = row
  forwardResult.value = undefined
  forwardSendSms.value = false
  forwardVisible.value = true
}
const submitForward = async () => {
  if (!forwardTarget.value?.id) return
  forwardSending.value = true
  try {
    const result = await SellerNotifyApi.forwardSettlementLink({
      settlementId: forwardTarget.value.id,
      sendSms: forwardSendSms.value
    })
    forwardResult.value = result
    message.success(result.message || '已生成')
  } catch (e: any) {
    message.error(e?.msg || '生成失败，请检查自然人端入口地址配置')
  } finally {
    forwardSending.value = false
  }
}
const copyForward = async (text: string) => {
  try {
    await navigator.clipboard.writeText(text)
    message.success('已复制')
  } catch {
    message.warning('复制失败，请手动选择文本')
  }
}

getList()
</script>
