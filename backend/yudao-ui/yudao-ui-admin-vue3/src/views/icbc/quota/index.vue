<template>
  <!-- 出售者额度台账：连续 12 个月滚动、跨租户合并 -->
  <ContentWrap title="出售者额度台账">
    <el-alert
      type="info"
      :closable="false"
      class="mb-10px"
      title="额度是自然人的，不是某个租户的：同一个出售者在别家回收企业开出的票也算在一起。连续 12 个月累计超过 500 万元就不能再反向开票，须引导其办理经营主体登记；月销售额超过 10 万元则须按时代办申报缴款。"
    />
    <el-form :inline="true" label-width="90px">
      <el-form-item label="出售者">
        <el-select
          v-model="payeeId"
          filterable
          remote
          reserve-keyword
          clearable
          placeholder="输入姓名 / 手机号搜索"
          :remote-method="searchPayee"
          :loading="payeeLoading"
          class="!w-320px"
          @change="handleQueryQuota"
        >
          <el-option
            v-for="item in payeeOptions"
            :key="item.id"
            :label="`${item.name}（${item.mobile || item.idCardNo || ''}）`"
            :value="item.id!"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :disabled="!payeeId" :loading="quotaLoading" @click="handleQueryQuota">
          查询额度
        </el-button>
        <el-button
          v-if="quota"
          plain
          @click="handleQuotaQrcode"
          v-hasPermi="['icbc:public-token:create']"
        >
          余量查询二维码
        </el-button>
      </el-form-item>
    </el-form>

    <template v-if="quota">
      <el-alert
        :type="quota.quotaExceeded ? 'error' : 'success'"
        :closable="false"
        :title="quota.message"
        class="mb-10px"
      />
      <el-descriptions :column="4" border size="small" class="mb-10px">
        <el-descriptions-item label="出售者">{{ quota.name }}</el-descriptions-item>
        <el-descriptions-item label="身份证">{{ quota.idCardMasked }}</el-descriptions-item>
        <el-descriptions-item label="上限(元)">{{ quota.capAmount }}</el-descriptions-item>
        <el-descriptions-item label="余量(元)">{{ quota.remainingAmount }}</el-descriptions-item>
        <el-descriptions-item label="已开票(元)">{{ quota.issuedAmount }}</el-descriptions-item>
        <el-descriptions-item label="在途(元)">{{ quota.pendingAmount }}</el-descriptions-item>
        <el-descriptions-item label="红冲(元)">{{ quota.redOffsetAmount }}</el-descriptions-item>
        <el-descriptions-item label="已用(元)">{{ quota.usedAmount }}</el-descriptions-item>
        <el-descriptions-item label="减按 1% 部分(元)">{{ quota.amountAtOnePercent }}</el-descriptions-item>
        <el-descriptions-item label="放弃减按 3% 部分(元)">{{ quota.amountAtThreePercent }}</el-descriptions-item>
        <el-descriptions-item label="本月销售额(元)">
          {{ quota.currentMonthAmount }} / 免征线 {{ quota.monthlyExemptAmount }}
          <el-tag v-if="quota.currentMonthOverExempt" type="warning" size="small" class="ml-1">
            已超免征线
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="滚动窗口">
          {{ formatTs(quota.windowStart) }} ~ {{ formatTs(quota.windowEnd) }}
        </el-descriptions-item>
      </el-descriptions>

      <el-table :data="quota.months || []" :stripe="true" size="small">
        <el-table-column label="月份" prop="month" width="100" />
        <el-table-column label="已开票(元)" align="right" prop="issuedAmount" />
        <el-table-column label="在途(元)" align="right" prop="pendingAmount" />
        <el-table-column label="红冲(元)" align="right" prop="redOffsetAmount" />
        <el-table-column label="净销售额(元)" align="right" prop="netAmount" />
        <el-table-column label="减按 1%(元)" align="right" prop="amountAtOnePercent" />
        <el-table-column label="放弃减按 3%(元)" align="right" prop="amountAtThreePercent" />
        <el-table-column label="其他征收率(元)" align="right" prop="otherAmount" />
        <el-table-column label="10 万免征线" align="center" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.overMonthlyExempt" type="warning" size="small">已超</el-tag>
            <span v-else>未超</span>
          </template>
        </el-table-column>
      </el-table>
    </template>
  </ContentWrap>

  <!-- 超限后的经营主体登记引导 -->
  <ContentWrap title="办理经营主体登记引导">
    <el-alert
      type="warning"
      :closable="false"
      class="mb-10px"
      title="出售者连续 12 个月销售额超过 500 万元后不能再反向开票。被拒的开票申请会在这里留下一条记录：联系出售者、引导其办理经营主体登记，办完即可结案。"
    />
    <el-form :inline="true" :model="guidanceQuery" label-width="80px">
      <el-form-item label="出售者">
        <el-input
          v-model="guidanceQuery.sellerName"
          placeholder="姓名"
          clearable
          class="!w-180px"
          @keyup.enter="handleGuidanceQuery"
        />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="guidanceQuery.status" clearable placeholder="全部" class="!w-160px">
          <el-option v-for="item in guidanceStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleGuidanceQuery"><Icon icon="ep:search" class="mr-5px" />搜索</el-button>
      </el-form-item>
    </el-form>
    <el-table v-loading="guidanceLoading" :data="guidanceList" :stripe="true">
      <el-table-column label="出售者" prop="sellerName" width="110" />
      <el-table-column label="身份证" prop="idCardMasked" width="180" />
      <el-table-column label="触发场景" width="120">
        <template #default="{ row }">{{ row.triggerSceneName || '-' }}</template>
      </el-table-column>
      <el-table-column label="触发业务单号" prop="triggerBizNo" min-width="180" />
      <el-table-column label="触发时已用额度(元)" align="right" prop="usedAmount" width="160" />
      <el-table-column label="状态" align="center" width="100">
        <template #default="{ row }">
          <el-tag :type="guidanceTagType(row.status)">{{ row.statusName || '-' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="最近触发" align="center" prop="lastTriggeredAt" :formatter="dateFormatter" width="170" />
      <el-table-column label="处理说明" prop="handleRemark" min-width="180" show-overflow-tooltip />
      <el-table-column label="操作" align="center" width="100" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.status !== 2"
            link
            type="primary"
            @click="openHandle(row)"
            v-hasPermi="['icbc:quota:guidance:handle']"
          >
            处理
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination
      :total="guidanceTotal"
      v-model:page="guidanceQuery.pageNo"
      v-model:limit="guidanceQuery.pageSize"
      @pagination="getGuidanceList"
    />
  </ContentWrap>

  <!-- 余量查询二维码：出售者零客户端自助查看 -->
  <el-dialog v-model="qrVisible" title="额度余量查询二维码" width="380px">
    <div class="qr-wrap">
      <Qrcode v-if="qrUrl" :text="qrUrl" :width="260" />
      <p class="qr-tip">出售者用手机扫开即可查看自己的额度余量，不需要装 App、不需要注册</p>
      <el-input v-model="qrUrl" readonly />
    </div>
  </el-dialog>

  <!-- 处理引导 -->
  <el-dialog v-model="handleVisible" title="处理引导" width="520px">
    <el-form :model="handleForm" label-width="100px">
      <el-form-item label="出售者">{{ currentGuidance?.sellerName }}</el-form-item>
      <el-form-item label="引导状态">
        <el-select v-model="handleForm.status" class="w-full">
          <el-option
            v-for="item in handleStatusOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="处理说明">
        <el-input v-model="handleForm.handleRemark" type="textarea" :rows="3" placeholder="如：已电话告知，出售者本周去办理个体工商户登记" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="handleVisible = false">取消</el-button>
      <el-button type="primary" :loading="handleLoading" @click="submitHandle">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { PayeeApi, PayeeVO } from '@/api/icbc/payee'
import { PublicTokenApi, buildPublicUrl } from '@/api/icbc/publicToken'
import { SellerQuotaApi, SellerQuotaGuidanceVO, SellerQuotaVO } from '@/api/icbc/quota'
import { Qrcode } from '@/components/Qrcode'
import { dateFormatter, formatDate } from '@/utils/formatTime'

/** 出售者额度台账与经营主体登记引导（#12） */
defineOptions({ name: 'IcbcQuota' })

const message = useMessage()

/** 后端把 LocalDateTime 按毫秒时间戳下发 */
const formatTs = (timestamp?: number) => (timestamp ? formatDate(new Date(timestamp)) : '-')

const payeeOptions = ref<PayeeVO[]>([])
const payeeLoading = ref(false)
const payeeId = ref<number>()
const quota = ref<SellerQuotaVO>()
const quotaLoading = ref(false)

const searchPayee = async (keyword: string) => {
  payeeLoading.value = true
  try {
    const data = await PayeeApi.getPayeePage({ pageNo: 1, pageSize: 20, name: keyword })
    payeeOptions.value = data.list
  } finally {
    payeeLoading.value = false
  }
}

const handleQueryQuota = async () => {
  if (!payeeId.value) {
    quota.value = undefined
    return
  }
  quotaLoading.value = true
  try {
    quota.value = await SellerQuotaApi.getSellerQuota(payeeId.value)
  } finally {
    quotaLoading.value = false
  }
}

const qrVisible = ref(false)
const qrUrl = ref('')
const handleQuotaQrcode = async () => {
  if (!payeeId.value) {
    return
  }
  const token = await PublicTokenApi.create({ purpose: 'QUOTA_QUERY', payeeId: payeeId.value })
  qrUrl.value = buildPublicUrl('quota', token.token!)
  qrVisible.value = true
}

// ==================== 引导清单 ====================

const guidanceLoading = ref(false)
const guidanceList = ref<SellerQuotaGuidanceVO[]>([])
const guidanceTotal = ref(0)
const guidanceQuery = reactive({
  pageNo: 1,
  pageSize: 10,
  sellerName: undefined,
  status: undefined
})
const guidanceStatusOptions = [
  { label: '待引导', value: 0 },
  { label: '已引导', value: 1 },
  { label: '已办结', value: 2 }
]
const handleStatusOptions = [
  { label: '已引导', value: 1 },
  { label: '已办结', value: 2 }
]
const guidanceTagType = (status?: number) => {
  if (status === 2) return 'success'
  if (status === 1) return 'warning'
  return 'danger'
}

const getGuidanceList = async () => {
  guidanceLoading.value = true
  try {
    const data = await SellerQuotaApi.getGuidancePage(guidanceQuery)
    guidanceList.value = data.list
    guidanceTotal.value = data.total
  } finally {
    guidanceLoading.value = false
  }
}
const handleGuidanceQuery = () => {
  guidanceQuery.pageNo = 1
  getGuidanceList()
}

const handleVisible = ref(false)
const handleLoading = ref(false)
const currentGuidance = ref<SellerQuotaGuidanceVO>()
const handleForm = reactive({ id: 0, status: 1, handleRemark: '' })

const openHandle = (row: SellerQuotaGuidanceVO) => {
  currentGuidance.value = row
  handleForm.id = row.id!
  handleForm.status = row.status === 0 ? 1 : 2
  handleForm.handleRemark = row.handleRemark || ''
  handleVisible.value = true
}

const submitHandle = async () => {
  handleLoading.value = true
  try {
    await SellerQuotaApi.handleGuidance(handleForm)
    message.success('已更新引导状态')
    handleVisible.value = false
    await getGuidanceList()
  } finally {
    handleLoading.value = false
  }
}

onMounted(() => {
  getGuidanceList()
})
</script>

<style scoped>
.qr-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}
.qr-tip {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  text-align: center;
}
</style>
