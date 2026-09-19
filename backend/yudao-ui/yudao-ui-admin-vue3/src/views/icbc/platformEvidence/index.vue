<template>
  <ContentWrap title="全平台证据与异常票">
    <el-alert
      type="info"
      :closable="false"
      class="mb-10px"
      title="全平台（跨租户）五流齐备率与异常票。异常票包含状态线异常的票，以及已开出但五流证据不齐的票。"
    />

    <el-row :gutter="12" class="mb-15px">
      <el-col :span="8">
        <el-card shadow="never">
          <div class="stat-label">统计票数</div>
          <div class="stat-value">{{ completeness.invoiceCount }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never">
          <div class="stat-label">五流齐备票数</div>
          <div class="stat-value text-green-600">{{ completeness.completeCount }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never">
          <div class="stat-label">批量齐备率</div>
          <div class="stat-value">{{ completeness.completenessRate }}%</div>
        </el-card>
      </el-col>
    </el-row>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="异常票清单" name="exception">
        <el-table v-loading="loadingException" :data="exceptionList" :stripe="true">
          <el-table-column label="租户编号" align="center" prop="tenantId" width="100" />
          <el-table-column label="合作方订单号" prop="partnerOrderId" min-width="180" show-overflow-tooltip />
          <el-table-column label="发票号码" prop="invoiceNo" min-width="180" show-overflow-tooltip />
          <el-table-column label="齐备率" align="center" width="90">
            <template #default="{ row }">
              <span v-if="row.completenessRate !== undefined">{{ row.completenessRate }}%</span>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="异常原因" min-width="320">
            <template #default="{ row }">
              <el-tag v-for="reason in row.reasons" :key="reason" type="danger" class="mr-5px mb-5px">
                {{ reason }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="逐票齐备率" name="completeness">
        <el-table v-loading="loadingCompleteness" :data="completeness.items" :stripe="true">
          <el-table-column label="租户编号" align="center" prop="tenantId" width="100" />
          <el-table-column label="合作方订单号" prop="partnerOrderId" min-width="180" show-overflow-tooltip />
          <el-table-column label="发票号码" prop="invoiceNo" min-width="180" show-overflow-tooltip />
          <el-table-column label="齐备流数" align="center" width="100">
            <template #default="{ row }">{{ row.presentCount }} / {{ row.totalCount }}</template>
          </el-table-column>
          <el-table-column label="齐备率" align="center" prop="completenessRate" width="90">
            <template #default="{ row }">{{ row.completenessRate }}%</template>
          </el-table-column>
          <el-table-column label="缺失的流" min-width="220">
            <template #default="{ row }">
              <el-tag v-for="flow in row.missingFlows" :key="flow" type="warning" class="mr-5px mb-5px">
                {{ flow }}
              </el-tag>
              <span v-if="!row.missingFlows || row.missingFlows.length === 0" class="text-green-600">齐备</span>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </ContentWrap>
</template>

<script setup lang="ts">
import {
  PlatformEvidenceApi,
  PlatformCompletenessVO,
  PlatformExceptionInvoiceVO
} from '@/api/icbc/platformEvidence'

defineOptions({ name: 'IcbcPlatformEvidence' })

const activeTab = ref('exception')
const loadingException = ref(true)
const loadingCompleteness = ref(true)
const exceptionList = ref<PlatformExceptionInvoiceVO[]>([])
const completeness = ref<PlatformCompletenessVO>({
  invoiceCount: 0,
  completeCount: 0,
  completenessRate: 0,
  items: []
})

const getData = async () => {
  loadingException.value = true
  loadingCompleteness.value = true
  try {
    const [exception, summary] = await Promise.all([
      PlatformEvidenceApi.getExceptionList(),
      PlatformEvidenceApi.getCompleteness()
    ])
    exceptionList.value = exception
    completeness.value = summary
  } finally {
    loadingException.value = false
    loadingCompleteness.value = false
  }
}

onMounted(getData)
</script>

<style scoped>
.stat-label {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
.stat-value {
  font-size: 24px;
  font-weight: 600;
  margin-top: 6px;
}
</style>
