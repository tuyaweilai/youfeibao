<template>
  <ContentWrap title="开票就绪自检">
    <el-alert
      type="info"
      :closable="false"
      class="mb-10px"
      title="开票能力取决于：资质有效、付方（子商户）档案就绪、出售者状态可用、额度未超。此页只汇总当前平台能查到的部分。"
    />

    <el-descriptions :column="2" border v-loading="loading">
      <el-descriptions-item label="适配层模式">{{ config.gatewayMode || '-' }}</el-descriptions-item>
      <el-descriptions-item label="工行连通性">
        <el-tag :type="connectivity?.reachable ? 'success' : 'danger'">
          {{ connectivity?.reachable ? '可达' : '不可达' }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="连通返回码">{{ connectivity?.returnCode ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="连通返回消息">{{ connectivity?.returnMsg || '-' }}</el-descriptions-item>
      <el-descriptions-item label="付方档案（子商户）数">{{ payerCount }}</el-descriptions-item>
      <el-descriptions-item label="企业资质数">{{ qualificationCount }}</el-descriptions-item>
    </el-descriptions>

    <el-table :data="checklist" border class="mt-15px">
      <el-table-column label="就绪项" prop="item" min-width="200" />
      <el-table-column label="状态" width="130" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status">{{ row.statusText }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="说明" prop="desc" min-width="360" />
    </el-table>

    <div class="mt-15px">
      <el-button @click="load" :loading="loading">重新自检</el-button>
      <el-button @click="go('/icbc/payer')">去付方档案</el-button>
      <el-button @click="go('/readiness/enterprise-info')">去企业信息</el-button>
      <el-button @click="go('/readiness/enterprise-qualification')">去企业资质</el-button>
    </div>
  </ContentWrap>
</template>

<script setup lang="ts">
import { IcbcTestApi, IcbcConnectivity } from '@/api/icbc/test'
import { PayerApi } from '@/api/icbc/payer'
import { QualificationApi } from '@/api/enterprise/qualification'

defineOptions({ name: 'IcbcReadiness' })

const router = useRouter()

const loading = ref(false)
const config = ref<Record<string, any>>({})
const connectivity = ref<IcbcConnectivity>()
const payerCount = ref(0)
const qualificationCount = ref(0)

const checklist = computed(() => [
  {
    item: '工行适配层连通性',
    status: connectivity.value?.reachable ? 'success' : 'danger',
    statusText: connectivity.value?.reachable ? '通过' : '未通过',
    desc: connectivity.value?.reachable
      ? '适配层已能打到工行网关。'
      : `不可达：${connectivity.value?.returnMsg || '未取到结果'}。本地 fake 模式下不可达属正常。`
  },
  {
    item: '付方档案（子商户）',
    status: payerCount.value > 0 ? 'success' : 'warning',
    statusText: payerCount.value > 0 ? '已配置' : '未配置',
    desc: '回收企业作为子商户的档案。未配置时无法发起开票与付款。'
  },
  {
    item: '企业资质',
    status: qualificationCount.value > 0 ? 'success' : 'warning',
    statusText: qualificationCount.value > 0 ? '已录入' : '未录入',
    desc: '税务侧反向开票资格、行业侧资质、公安侧备案三类。任一层失效即冻结开票。'
  },
  {
    item: '企业授权（工行）',
    status: 'info',
    statusText: '平台待建设',
    desc: '需法定代表人或财务负责人用税务 App 扫码实人认证。接口在适配层已就位，管理端点待 #5 建。'
  },
  {
    item: '编码配置（品类 / 税收分类编码）',
    status: 'info',
    statusText: '平台待建设',
    desc: '品类、计量单位、税率、商品和服务税收分类合并编码配置化。见 #5。'
  }
])

const load = async () => {
  loading.value = true
  try {
    const [cfg, conn, payers, quals] = await Promise.allSettled([
      IcbcTestApi.getConfig(),
      IcbcTestApi.checkConnectivity(),
      PayerApi.getPayerPage({ pageNo: 1, pageSize: 1 }),
      QualificationApi.getQualificationPage({ pageNo: 1, pageSize: 1 })
    ])
    if (cfg.status === 'fulfilled') config.value = cfg.value || {}
    if (conn.status === 'fulfilled') connectivity.value = conn.value
    if (payers.status === 'fulfilled') payerCount.value = payers.value?.total ?? 0
    if (quals.status === 'fulfilled') qualificationCount.value = quals.value?.total ?? 0
  } finally {
    loading.value = false
  }
}

const go = (path: string) => router.push(path)

onMounted(load)
</script>
