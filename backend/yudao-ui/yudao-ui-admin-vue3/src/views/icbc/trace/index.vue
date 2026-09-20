<template>
  <ContentWrap>
    <el-alert
      type="info"
      :closable="false"
      show-icon
      class="mb-10px"
      title="一批货经历了什么：采购订单 → 现场收货 → 仓储入库 → 结算确认，后接付款与发票。入库量与结算量不默认一对一——差额或缺失关联会在详情里逐条说明。"
    />
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="90px">
      <el-form-item label="查号方式" prop="keywordType">
        <el-select v-model="queryParams.keywordType" class="!w-220px">
          <el-option
            v-for="item in TRACE_KEYWORD_TYPE_OPTIONS"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="单号 / 车牌 / 主体" prop="keyword">
        <el-input
          v-model="queryParams.keyword"
          placeholder="输入单号或车牌或姓名 / 手机号"
          clearable
          class="!w-260px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="车牌" prop="plateNo">
        <el-input
          v-model="queryParams.plateNo"
          placeholder="京A12345"
          clearable
          class="!w-160px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="出售者" prop="sellerName">
        <el-input
          v-model="queryParams.sellerName"
          placeholder="姓名 / 手机号"
          clearable
          class="!w-160px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="交易时间" prop="tradeTime">
        <el-date-picker
          v-model="queryParams.tradeTime"
          type="datetimerange"
          value-format="x"
          range-separator="至"
          start-placeholder="开始"
          end-placeholder="结束"
          class="!w-360px"
        />
      </el-form-item>
      <el-form-item label="只看差异" prop="onlyDifference">
        <el-switch v-model="queryParams.onlyDifference" />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 查询</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button
          type="primary"
          plain
          :loading="exporting"
          @click="handleExport"
          v-hasPermi="['icbc:trace:export']"
        >
          <Icon icon="ep:download" class="mr-5px" /> 导出
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap v-if="summary || scopeNote">
    <el-alert type="success" :closable="false" class="mb-10px">
      <template #title>
        <span>{{ scopeNote }}</span>
      </template>
      <div v-if="summary">
        汇总（每张收购单只计一次）：{{ summary.acquisitionCount }} 张 · 结算重量
        {{ summary.totalSettlementWeight ?? 0 }} · 已入库 {{ summary.totalStockedWeight ?? 0 }} · 金额
        {{ summary.totalAmount ?? 0 }} 元 · 有差异 {{ summary.differenceCount ?? 0 }} 张
        <div class="text-12px text-gray-500">{{ summary.countNote }}</div>
        <div v-if="hiddenDetailCount > 0" class="text-12px text-orange-500">
          另有 {{ hiddenDetailCount }} 张命中明细未在本页展示。
        </div>
      </div>
    </el-alert>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true">
      <el-table-column label="收购单号" prop="acquisitionNo" width="210" fixed="left">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">{{ row.acquisitionNo }}</el-button>
        </template>
      </el-table-column>
      <el-table-column label="交接批次" prop="handoverBatchNo" width="170" />
      <el-table-column label="出售者" prop="sellerName" width="100" />
      <el-table-column label="品类" width="140">
        <template #default="{ row }">{{ row.categoryName }}{{ row.unit ? ` / ${row.unit}` : '' }}</template>
      </el-table-column>
      <el-table-column label="车牌" prop="plateNo" width="110" />
      <el-table-column label="实物量" prop="physicalWeight" align="right" width="100" />
      <el-table-column label="结算重量" prop="settlementWeight" align="right" width="110" />
      <el-table-column label="已入库" prop="stockedWeight" align="right" width="100" />
      <el-table-column label="金额(元)" prop="amount" align="right" width="110" />
      <el-table-column label="采购订单" width="130">
        <template #default="{ row }">
          <el-tag v-if="row.directAcquisition" type="info" size="small">直接收购</el-tag>
          <span v-else>{{ stageOf(row, 'PURCHASE_ORDER')?.nodes?.[0]?.bizNo || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="入库" width="90">
        <template #default="{ row }">
          <el-tag :type="traceStageStatusTag(stageOf(row, 'STOCK_IN')?.status)" size="small">
            {{ stageOf(row, 'STOCK_IN')?.statusName || '—' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="结算确认" width="110">
        <template #default="{ row }">
          <el-tag :type="traceStageStatusTag(stageOf(row, 'SETTLEMENT')?.status)" size="small">
            {{ stageOf(row, 'SETTLEMENT')?.statusName || '—' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="付款" width="90">
        <template #default="{ row }">
          <el-tag :type="traceStageStatusTag(stageOf(row, 'PAYMENT')?.status)" size="small">
            {{ stageOf(row, 'PAYMENT')?.statusName || '—' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="发票" width="90">
        <template #default="{ row }">
          <el-tag :type="traceStageStatusTag(stageOf(row, 'INVOICE')?.status)" size="small">
            {{ stageOf(row, 'INVOICE')?.statusName || '—' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="差异" width="90">
        <template #default="{ row }">
          <el-tag v-if="row.differences && row.differences.length" type="danger" size="small">
            {{ row.differences.length }} 条
          </el-tag>
          <el-tag v-else type="success" size="small">无</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="90" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </ContentWrap>

  <!-- 详情：四栏 + 付款 / 发票 -->
  <el-drawer v-model="detailVisible" title="关联单据详情" size="70%" :destroy-on-close="true">
    <div v-if="detail" v-loading="detailLoading">
      <el-descriptions :column="2" border class="mb-10px">
        <el-descriptions-item label="收购单号">{{ detail.acquisitionNo }}</el-descriptions-item>
        <el-descriptions-item label="交接批次">{{ detail.handoverBatchNo || '—' }}</el-descriptions-item>
        <el-descriptions-item label="出售者">{{ detail.sellerName }}</el-descriptions-item>
        <el-descriptions-item label="身份证件">{{ detail.sensitive?.sellerIdCard || '—' }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ detail.sensitive?.sellerMobile || '—' }}</el-descriptions-item>
        <el-descriptions-item label="银行卡">{{ detail.sensitive?.sellerBankCard || '—' }}</el-descriptions-item>
        <el-descriptions-item label="回收企业税号">{{ detail.sensitive?.buyerTaxNo || '—' }}</el-descriptions-item>
        <el-descriptions-item label="车牌">{{ detail.plateNo || '—' }}</el-descriptions-item>
      </el-descriptions>

      <el-alert
        v-for="diff in detail.differences"
        :key="diff.code"
        type="warning"
        :closable="false"
        show-icon
        class="mb-8px"
        :title="`${diff.name}：${diff.note}`"
      />

      <el-card
        v-for="stage in detail.stages"
        :key="stage.code"
        shadow="never"
        class="mb-10px"
      >
        <template #header>
          <div class="flex justify-between items-center">
            <span class="font-bold">{{ stage.name }}</span>
            <el-tag :type="traceStageStatusTag(stage.status)" size="small">
              {{ stage.statusName }}
            </el-tag>
          </div>
        </template>
        <div class="text-12px text-gray-500 mb-5px">{{ stage.definition }}</div>
        <div class="mb-5px">
          数量 {{ stage.quantity ?? '—' }} · 重量 {{ stage.weight ?? '—' }} · 金额
          {{ stage.amount ?? '—' }} · 单位 {{ stage.unit || '—' }}
        </div>
        <el-alert v-if="stage.note" type="info" :closable="false" class="mb-8px" :title="stage.note" />
        <el-table v-if="stage.nodes && stage.nodes.length" :data="stage.nodes" size="small" :stripe="true">
          <el-table-column label="编号" prop="bizNo" min-width="180">
            <template #default="{ row }">
              <el-link v-if="row.detailPath" type="primary" @click="gotoDetail(row.detailPath)">
                {{ row.bizNo || row.title }}
              </el-link>
              <span v-else>{{ row.bizNo || row.title }}</span>
            </template>
          </el-table-column>
          <el-table-column label="标题" prop="title" min-width="140" />
          <el-table-column label="状态" prop="statusName" width="120" />
          <el-table-column label="数量" prop="quantity" align="right" width="100" />
          <el-table-column label="重量" prop="weight" align="right" width="100" />
          <el-table-column label="金额(元)" prop="amount" align="right" width="110" />
          <el-table-column label="时间" width="170">
            <template #default="{ row }">{{ row.time ? formatDate(row.time) : '—' }}</template>
          </el-table-column>
        </el-table>
        <el-empty v-else description="本环节暂无单据" :image-size="60" />
      </el-card>

      <el-card shadow="never" class="mb-10px">
        <template #header><span class="font-bold">附件</span></template>
        <el-empty v-if="!detail.attachments?.length" description="无附件" :image-size="60" />
        <div v-for="file in detail.attachments" :key="file.url">
          <el-link :href="file.url" target="_blank" type="primary">
            {{ file.name }}（{{ file.sourceNo || file.sourceType }}）
          </el-link>
        </div>
      </el-card>

      <el-card shadow="never">
        <template #header><span class="font-bold">操作历史（按业务单据时间线）</span></template>
        <el-timeline>
          <el-timeline-item
            v-for="(item, index) in detail.histories"
            :key="index"
            :timestamp="formatDate(item.time)"
          >
            [{{ item.stageName }}] {{ item.action }}<span v-if="item.detail"> · {{ item.detail }}</span>
          </el-timeline-item>
        </el-timeline>
      </el-card>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { TraceApi, TraceRowVO, TraceSummaryVO, traceStageStatusTag, TRACE_KEYWORD_TYPE_OPTIONS } from '@/api/icbc/trace'
import { formatDate } from '@/utils/formatTime'
import { checkPermi } from '@/utils/permission'

defineOptions({ name: 'IcbcTrace' })

const loading = ref(false)
const exporting = ref(false)
const list = ref<TraceRowVO[]>([])
const total = ref(0)
const summary = ref<TraceSummaryVO>()
const scopeNote = ref('')
const hiddenDetailCount = ref(0)
const queryFormRef = ref()

const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  keywordType: 'AUTO',
  keyword: undefined as string | undefined,
  plateNo: undefined as string | undefined,
  sellerName: undefined as string | undefined,
  tradeTime: undefined as number[] | undefined,
  onlyDifference: undefined as boolean | undefined
})

const stageOf = (row: TraceRowVO, code: string) =>
  row.stages?.find((stage) => stage.code === code)

const getList = async () => {
  if (!queryParams.keyword && !queryParams.plateNo && !queryParams.sellerName && !queryParams.tradeTime) {
    list.value = []
    total.value = 0
    summary.value = undefined
    scopeNote.value = ''
    return
  }
  loading.value = true
  try {
    const data = await TraceApi.search(queryParams)
    list.value = data.list
    total.value = data.total
    summary.value = data.summary
    scopeNote.value = data.scopeNote || ''
    hiddenDetailCount.value = data.hiddenDetailCount || 0
  } finally {
    loading.value = false
  }
}
const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}
const resetQuery = () => {
  queryFormRef.value?.resetFields()
  queryParams.keyword = undefined
  queryParams.plateNo = undefined
  queryParams.sellerName = undefined
  queryParams.tradeTime = undefined
  queryParams.onlyDifference = undefined
  handleQuery()
}

const handleExport = async () => {
  exporting.value = true
  try {
    await TraceApi.export(queryParams)
  } finally {
    exporting.value = false
  }
}

// ==================== 详情 ====================

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<TraceRowVO>()

const openDetail = async (row: TraceRowVO) => {
  detailVisible.value = true
  detailLoading.value = true
  try {
    // 有未脱敏权限时取原值，否则取脱敏值（同一个详情结构）
    detail.value = checkPermi(['icbc:trace:sensitive:view'])
      ? await TraceApi.getSensitive(row.acquisitionId!)
      : await TraceApi.get(row.acquisitionId!)
  } finally {
    detailLoading.value = false
  }
}

const gotoDetail = (path?: string) => {
  if (path) {
    window.location.href = `/#${path}`
  }
}
</script>
