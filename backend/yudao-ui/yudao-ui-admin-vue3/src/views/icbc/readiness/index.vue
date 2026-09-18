<template>
  <ContentWrap title="开票就绪自检">
    <el-alert
      type="info"
      :closable="false"
      class="mb-10px"
      title="开票能力取决于：三层资质有效、企业授权完成、付方（子商户）档案就绪、编码配置齐备、出售者状态可用、额度未超。此页汇总平台此刻能查到的部分。"
    />

    <el-descriptions :column="2" border v-loading="loading">
      <el-descriptions-item label="适配层模式">{{ config.gatewayMode || '-' }}</el-descriptions-item>
      <el-descriptions-item label="工行连通性">
        <el-tag :type="connectivity?.reachable ? 'success' : 'danger'">
          {{ connectivity?.reachable ? '可达' : '不可达' }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="三层资质齐全有效">
        <el-tag :type="tenantReady ? 'success' : 'danger'">{{ tenantReady ? '是' : '否' }}</el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="临近到期资质数（30 天）">{{ expiringCount }}</el-descriptions-item>
      <el-descriptions-item label="待处理到期预警">
        <el-tag :type="warnings.length > 0 ? 'warning' : 'success'">{{ warnings.length }}</el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="付方档案（子商户）数">{{ payerCount }}</el-descriptions-item>
      <el-descriptions-item label="启用品类数">{{ goodsCount }}</el-descriptions-item>
      <el-descriptions-item label="企业授权记录数">{{ authCount }}</el-descriptions-item>
      <el-descriptions-item label="已授权记录数">{{ authApprovedCount }}</el-descriptions-item>
    </el-descriptions>

    <el-table :data="checklist" border class="mt-15px">
      <el-table-column label="就绪项" prop="item" min-width="180" />
      <el-table-column label="状态" width="130" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status">{{ row.statusText }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="说明" prop="desc" min-width="360" />
    </el-table>

    <div class="mt-15px" v-if="warnings.length > 0">
      <el-alert
        type="warning"
        :closable="false"
        class="mb-10px"
        title="以下资质由定时任务扫描到临近到期，请尽快更新；处理后可关闭预警。"
      />
      <el-table :data="warnings" border>
        <el-table-column label="资质层" prop="type" width="140" />
        <el-table-column label="资质名称" prop="name" min-width="200" />
        <el-table-column label="有效期止" prop="validTo" width="140" />
        <el-table-column label="操作" align="center" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="ackWarning(row.id)" v-hasPermi="['icbc:expiry-warning:ack']">
              标为已处理
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="mt-15px">
      <el-button @click="load" :loading="loading">重新自检</el-button>
      <el-button @click="go('/readiness/qualification')">去三层资质</el-button>
      <el-button @click="go('/readiness/enterprise-auth')">去企业授权</el-button>
      <el-button @click="go('/readiness/goods-config')">去编码配置</el-button>
      <el-button @click="go('/icbc/payer')">去付方档案</el-button>
    </div>
  </ContentWrap>
</template>

<script setup lang="ts">
import { IcbcTestApi, IcbcConnectivity } from '@/api/icbc/test'
import { PayerApi } from '@/api/icbc/payer'
import { QualificationApi } from '@/api/icbc/qualification'
import { GoodsConfigApi } from '@/api/icbc/goodsConfig'
import { EnterpriseAuthApi } from '@/api/icbc/enterpriseAuth'
import { ExpiryWarningApi, ExpiryWarningVO } from '@/api/icbc/expiryWarning'

defineOptions({ name: 'IcbcReadiness' })

const router = useRouter()

const loading = ref(false)
const config = ref<Record<string, any>>({})
const connectivity = ref<IcbcConnectivity>()
const tenantReady = ref(false)
const expiringCount = ref(0)
const payerCount = ref(0)
const goodsCount = ref(0)
const authCount = ref(0)
const authApprovedCount = ref(0)
const warnings = ref<ExpiryWarningVO[]>([])

const checklist = computed(() => [
  {
    item: '三层资质（税务 / 行业 / 公安）',
    status: tenantReady.value ? 'success' : 'danger',
    statusText: tenantReady.value ? '齐全有效' : '缺失或失效',
    desc: '任一层失效即冻结开票。临近到期需提前更新。'
  },
  {
    item: '企业授权（工行）',
    status: authApprovedCount.value > 0 ? 'success' : 'warning',
    statusText: authApprovedCount.value > 0 ? '已授权' : '未授权',
    desc: '法定代表人或财务负责人用税务 App 扫码实人认证并录入授权有效期。'
  },
  {
    item: '付方档案（子商户）',
    status: payerCount.value > 0 ? 'success' : 'warning',
    statusText: payerCount.value > 0 ? '已配置' : '未配置',
    desc: '回收企业作为子商户的档案。未配置时无法发起开票与付款。'
  },
  {
    item: '编码配置（品类 / 税收分类编码）',
    status: goodsCount.value > 0 ? 'success' : 'warning',
    statusText: goodsCount.value > 0 ? '已配置' : '未配置',
    desc: '品类、计量单位、税率、商品和服务税收分类合并编码。开票申请按此带出。'
  },
  {
    item: '工行适配层连通性',
    status: connectivity.value?.reachable ? 'success' : 'danger',
    statusText: connectivity.value?.reachable ? '通过' : '未通过',
    desc: connectivity.value?.reachable
      ? '适配层已能打到工行网关。'
      : `不可达：${connectivity.value?.returnMsg || '未取到结果'}。本地 fake 模式下不可达属正常。`
  }
])

const load = async () => {
  loading.value = true
  try {
    const [cfg, conn, ready, expiring, payers, goods, auths, warns] = await Promise.allSettled([
      IcbcTestApi.getConfig(),
      IcbcTestApi.checkConnectivity(),
      QualificationApi.isTenantReady(),
      QualificationApi.getExpiring(30),
      PayerApi.getPayerPage({ pageNo: 1, pageSize: 1 }),
      GoodsConfigApi.getEnabledList(),
      EnterpriseAuthApi.getEnterpriseAuthPage({ pageNo: 1, pageSize: 100 }),
      ExpiryWarningApi.getOpenList()
    ])
    if (cfg.status === 'fulfilled') config.value = cfg.value || {}
    if (conn.status === 'fulfilled') connectivity.value = conn.value
    if (ready.status === 'fulfilled') tenantReady.value = !!ready.value
    if (expiring.status === 'fulfilled') expiringCount.value = (expiring.value || []).length
    if (payers.status === 'fulfilled') payerCount.value = payers.value?.total ?? 0
    if (goods.status === 'fulfilled') goodsCount.value = (goods.value || []).length
    if (auths.status === 'fulfilled') {
      const list = auths.value?.list || []
      authCount.value = auths.value?.total ?? list.length
      authApprovedCount.value = list.filter((a: any) => a.authStatus === 1).length
    }
    if (warns.status === 'fulfilled') warnings.value = warns.value || []
  } finally {
    loading.value = false
  }
}

const ackWarning = async (id?: number) => {
  if (!id) return
  await ExpiryWarningApi.acknowledge(id)
  await load()
}

const go = (path: string) => router.push(path)

onMounted(load)
</script>
