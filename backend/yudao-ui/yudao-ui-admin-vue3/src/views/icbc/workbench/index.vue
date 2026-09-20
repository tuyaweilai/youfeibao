<template>
  <div>
    <!-- 开票就绪徽标：一级菜单「租户开票就绪」取消后就绪状态降级到这里，点开看检查项 -->
    <ContentWrap>
      <div class="flex items-center justify-between lt-sm:flex-col lt-sm:items-start">
        <div class="flex flex-wrap items-center">
          <span class="mr-12px text-16px font-bold">今日待办与预警</span>
          <el-tag
            :type="readiness.ready ? 'success' : 'danger'"
            class="cursor-pointer"
            @click="readinessVisible = true"
          >
            <Icon :icon="readiness.ready ? 'ep:circle-check' : 'ep:warning'" class="mr-4px" />
            开票就绪：{{ readiness.ready ? '已就绪' : `待补齐 ${unreadyReadiness.length} 项` }}
          </el-tag>
          <el-button link type="primary" class="ml-10px" @click="readinessVisible = true">
            查看就绪详情
          </el-button>
        </div>
        <el-button :loading="loading" @click="load">
          <Icon icon="ep:refresh" class="mr-5px" /> 刷新
        </el-button>
      </div>
      <div class="mt-8px text-12px text-gray-500">
        数字全部来自本企业自己的单据；每项都写着口径，点「明细」看来源。
      </div>
    </ContentWrap>

    <!-- 待办 -->
    <ContentWrap title="待办">
      <el-row :gutter="12" v-loading="loading">
        <el-col
          v-for="todo in todos"
          :key="todo.code"
          :xs="24"
          :sm="12"
          :md="8"
          :lg="6"
          class="mb-12px"
        >
          <el-card shadow="hover" :body-style="{ padding: '14px' }">
            <div class="flex items-start justify-between">
              <div class="flex items-center">
                <span class="text-14px font-bold">{{ todo.name }}</span>
                <el-tag v-if="todo.available === false" type="info" size="small" class="ml-6px">
                  待接入
                </el-tag>
              </div>
              <span
                class="text-22px font-bold"
                :class="
                  todo.available === false ? 'text-gray-400' : todo.total ? 'text-red-500' : 'text-green-600'
                "
              >
                {{ todo.available === false ? '—' : todo.total }}
              </span>
            </div>

            <el-tooltip :content="todo.definition || ''" placement="top">
              <div class="mt-6px h-32px overflow-hidden text-12px text-gray-500 leading-16px">
                {{ todo.available === false ? todo.unavailableReason : todo.definition }}
              </div>
            </el-tooltip>

            <div class="mt-8px flex items-center justify-between">
              <el-button
                link
                type="primary"
                :disabled="todo.available === false"
                @click="openTodo(todo)"
              >
                明细
              </el-button>
              <el-button link type="primary" @click="go(ROUTES[todo.code || ''])">
                去处理 <Icon icon="ep:right" class="ml-2px" />
              </el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </ContentWrap>

    <!-- 预警 -->
    <ContentWrap title="预警">
      <el-row :gutter="12" v-loading="loading">
        <el-col v-for="warning in warnings" :key="warning.code" :xs="24" :md="8" class="mb-12px">
          <el-card shadow="hover" :body-style="{ padding: '14px' }">
            <div class="flex items-center justify-between">
              <span class="text-14px font-bold">{{ warning.name }}</span>
              <el-tag :type="tagType(warning.level)">{{ warning.count }}</el-tag>
            </div>
            <div class="mt-6px h-48px overflow-hidden text-12px text-gray-500 leading-16px">
              {{ warning.message }}
            </div>
            <div class="mt-8px flex items-center justify-between">
              <el-button
                link
                type="primary"
                :disabled="!(warning.items || []).length"
                @click="openWarning(warning)"
              >
                明细
              </el-button>
              <el-button link type="primary" @click="go(WARNING_ROUTES[warning.code || ''])">
                去处理 <Icon icon="ep:right" class="ml-2px" />
              </el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </ContentWrap>

    <!-- 待办 / 预警的来源明细 -->
    <el-drawer v-model="drawerVisible" :title="drawerTitle" size="640px">
      <el-alert v-if="drawerNote" type="info" :closable="false" class="mb-10px" :title="drawerNote" />
      <el-table :data="drawerItems" :stripe="true" v-loading="loading">
        <el-table-column label="单号 / 类型" prop="no" min-width="150" show-overflow-tooltip />
        <el-table-column label="谁 / 哪一笔" prop="title" min-width="110" show-overflow-tooltip />
        <el-table-column label="说明" prop="subtitle" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" prop="statusName" width="110" />
        <el-table-column label="时间" prop="time" :formatter="dateFormatter" width="165" />
        <el-table-column label="金额" prop="amount" width="110" align="right" />
      </el-table>
      <el-empty v-if="!loading && drawerItems.length === 0" description="此项当前没有待处理明细" />
      <div v-if="drawerRoute" class="mt-10px text-right">
        <el-button type="primary" link @click="go(drawerRoute)">去处理全部</el-button>
      </div>
    </el-drawer>

    <!-- 开票就绪自检 -->
    <el-dialog v-model="readinessVisible" title="开票就绪自检" width="680px">
      <el-alert
        type="info"
        :closable="false"
        class="mb-10px"
        title="只列本地库能判定的部分（三层资质 / 企业授权 / 付方档案 / 编码配置）；工行适配层连通性在「基础资料 - 开票就绪自检」页按需验证。"
      />
      <el-table :data="readiness.items || []" :stripe="true">
        <el-table-column label="检查项" prop="name" min-width="180" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.ready ? 'success' : 'danger'">
              {{ row.ready ? '已具备' : '待补齐' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="说明" prop="message" min-width="260" />
        <el-table-column label="操作" width="100" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="go(READINESS_ROUTES[row.code])">去处理</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import {
  WorkbenchApi,
  WorkbenchItemVO,
  WorkbenchOverviewVO,
  WorkbenchReadinessVO,
  WorkbenchTodoVO,
  WorkbenchWarningVO
} from '@/api/icbc/workbench'

defineOptions({ name: 'IcbcWorkbench' })

// 下钻目标由前端持有：后端只给 code，路由是展示层的事。
const ROUTES: Record<string, string> = {
  ARRIVAL_TODAY: '/recycling/appointment',
  PENDING_WEIGH: '/recycling/acquisition',
  PENDING_INSPECT: '/recycling/acquisition',
  PENDING_STOCK_IN: '/warehouse/inventory',
  PENDING_SETTLE_CONFIRM: '/settlement/list',
  SETTLE_DISPUTE: '/settlement/list',
  PAYMENT_FAILED: '/finance/payment',
  INVOICE_FAILED: '/finance/invoice-application'
}

const WARNING_ROUTES: Record<string, string> = {
  QUOTA: '/finance/quota',
  QUALIFICATION_EXPIRY: '/basedata/self-check',
  READINESS: '/basedata/self-check'
}

const READINESS_ROUTES: Record<string, string> = {
  QUALIFICATION: '/basedata/qualification',
  ENTERPRISE_AUTH: '/counterparty/enterprise-auth',
  PAYER: '/basedata/payer',
  GOODS_CONFIG: '/basedata/goods-config'
}

const router = useRouter()

const loading = ref(false)
const overview = ref<WorkbenchOverviewVO>({})

const todos = computed<WorkbenchTodoVO[]>(() => overview.value.todos || [])
const warnings = computed<WorkbenchWarningVO[]>(() => overview.value.warnings || [])
const readiness = computed<WorkbenchReadinessVO>(() => overview.value.readiness || {})
const unreadyReadiness = computed(() => (readiness.value.items || []).filter((item) => !item.ready))

const drawerVisible = ref(false)
const drawerTitle = ref('')
const drawerNote = ref('')
const drawerItems = ref<WorkbenchItemVO[]>([])
const drawerRoute = ref('')
const readinessVisible = ref(false)

const load = async () => {
  loading.value = true
  try {
    overview.value = (await WorkbenchApi.getOverview()) || {}
  } finally {
    loading.value = false
  }
}

const openTodo = (todo: WorkbenchTodoVO) => {
  drawerTitle.value = `${todo.name} · 共 ${todo.total} 条`
  drawerNote.value = todo.definition || ''
  drawerItems.value = todo.items || []
  drawerRoute.value = ROUTES[todo.code || ''] || ''
  drawerVisible.value = true
}

const openWarning = (warning: WorkbenchWarningVO) => {
  drawerTitle.value = `${warning.name} · 共 ${warning.count} 条`
  drawerNote.value = warning.message || ''
  drawerItems.value = warning.items || []
  drawerRoute.value = WARNING_ROUTES[warning.code || ''] || ''
  drawerVisible.value = true
}

const tagType = (level?: string) => {
  if (level === 'DANGER') return 'danger'
  if (level === 'WARN') return 'warning'
  return 'success'
}

const go = (path?: string) => {
  if (!path) return
  router.push(path)
}

onMounted(load)
</script>
