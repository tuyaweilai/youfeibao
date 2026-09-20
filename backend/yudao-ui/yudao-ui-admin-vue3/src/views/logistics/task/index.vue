<template>
  <ContentWrap>
    <el-alert
      type="info"
      :closable="false"
      class="mb-10px"
      title="一个运输任务 = 一车 + 一司机 + 一次执行（一个出发地、一个提货点、一个时间窗）。任务可以什么都不挂——司机直接上门收购是常态。本票只开放「起运」一个节点；其余四类节点与异常标记见后续票。"
    />
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="80px">
      <el-form-item label="任务单号" prop="taskNo">
        <el-input v-model="queryParams.taskNo" placeholder="请输入" clearable @keyup.enter="handleQuery" class="!w-200px" />
      </el-form-item>
      <el-form-item label="车牌号" prop="plateNo">
        <el-input v-model="queryParams.plateNo" placeholder="请输入" clearable @keyup.enter="handleQuery" class="!w-160px" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择" clearable class="!w-160px">
          <el-option v-for="(label, value) in STATUS_NAME" :key="value" :label="label" :value="Number(value)" />
        </el-select>
      </el-form-item>
      <el-form-item label="提货点" prop="pickupAddress">
        <el-input v-model="queryParams.pickupAddress" placeholder="请输入" clearable @keyup.enter="handleQuery" class="!w-200px" />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" plain @click="openForm('create')" v-hasPermi="['logistics:transport-task:create']">
          <Icon icon="ep:plus" class="mr-5px" /> 派车
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true">
      <el-table-column label="任务单号" prop="taskNo" min-width="190" />
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template #default="{ row }">
          <el-tag :type="STATUS_TAG[row.status] || 'info'">{{ row.statusName || '未知' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="车牌号" prop="plateNo" min-width="110" />
      <el-table-column label="司机" prop="driverName" min-width="100" />
      <el-table-column label="提货点" prop="pickupAddress" min-width="180" show-overflow-tooltip />
      <el-table-column label="时间窗" min-width="230">
        <template #default="{ row }">
          {{ formatTime(row.expectedStartTime) }} ~ {{ formatTime(row.expectedEndTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="260" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row.id)">详情与时间线</el-button>
          <el-button
            v-if="row.status === 0"
            link
            type="primary"
            @click="openAssign(row)"
            v-hasPermi="['logistics:transport-task:assign']"
          >派车</el-button>
          <el-button
            v-if="row.status === 1"
            link
            type="primary"
            @click="handleAccept(row.id)"
            v-hasPermi="['logistics:transport-task:update']"
          >接单</el-button>
          <el-button
            v-if="row.status === 3 || row.status === 2"
            link
            type="success"
            @click="handleComplete(row.id)"
            v-hasPermi="['logistics:transport-task:update']"
          >完成</el-button>
          <el-button
            v-if="row.status !== 4 && row.status !== 5"
            link
            type="danger"
            @click="openCancel(row)"
            v-hasPermi="['logistics:transport-task:cancel']"
          >取消</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </ContentWrap>

  <!-- 派车 / 新建 -->
  <Dialog :title="dialogTitle" v-model="dialogVisible" width="620px">
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="110px" v-loading="formLoading">
      <el-form-item label="提货点地址" prop="pickupAddress">
        <el-input v-model="formData.pickupAddress" placeholder="如：某某路 1 号" />
      </el-form-item>
      <el-form-item label="出发地" prop="departureAddress">
        <el-input v-model="formData.departureAddress" placeholder="通常是场站或车队所在地" />
      </el-form-item>
      <el-form-item label="提货联系人" prop="pickupContactName">
        <el-input v-model="formData.pickupContactName" placeholder="选填" />
      </el-form-item>
      <el-form-item label="联系电话" prop="pickupContactPhone">
        <el-input v-model="formData.pickupContactPhone" placeholder="选填" />
      </el-form-item>
      <el-form-item label="时间窗" prop="expectedStartTime">
        <el-date-picker
          v-model="timeWindow"
          type="datetimerange"
          value-format="x"
          start-placeholder="开始"
          end-placeholder="结束"
          class="!w-100%"
        />
      </el-form-item>
      <el-form-item label="车辆" prop="vehicleId">
        <el-select v-model="formData.vehicleId" placeholder="留空即待分配" clearable filterable class="!w-100%">
          <el-option
            v-for="v in vehicleOptions"
            :key="v.id"
            :label="`${v.plateNo}${v.vehicleType ? ' · ' + v.vehicleType : ''}`"
            :value="v.id!"
            :disabled="v.status === 2"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="司机" prop="driverId">
        <el-select v-model="formData.driverId" placeholder="留空即待分配" clearable filterable class="!w-100%">
          <el-option
            v-for="d in driverOptions"
            :key="d.id"
            :label="`${d.name}${d.source === 2 ? '（承运商）' : ''}`"
            :value="d.id!"
            :disabled="d.status !== 0"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="采购安排" prop="purchaseOrderNo">
        <el-input v-model="formData.purchaseOrderNo" placeholder="选填：关联的采购订单号；不挂也能派车" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" type="textarea" :rows="2" placeholder="选填" />
      </el-form-item>
      <div class="tip">车辆与司机要么都选、要么都不选：只给一个等于半套派车，会被拦下。</div>
    </el-form>
    <template #footer>
      <el-button @click="submitForm" type="primary" :disabled="formLoading">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>

  <!-- 派车（待分配的任务） -->
  <Dialog title="派车" v-model="assignVisible" width="480px">
    <el-form label-width="90px">
      <el-form-item label="车辆">
        <el-select v-model="assignForm.vehicleId" placeholder="请选择" filterable class="!w-100%">
          <el-option
            v-for="v in vehicleOptions"
            :key="v.id"
            :label="`${v.plateNo}${v.vehicleType ? ' · ' + v.vehicleType : ''}`"
            :value="v.id!"
            :disabled="v.status === 2"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="司机">
        <el-select v-model="assignForm.driverId" placeholder="请选择" filterable class="!w-100%">
          <el-option
            v-for="d in driverOptions"
            :key="d.id"
            :label="`${d.name}${d.source === 2 ? '（承运商）' : ''}`"
            :value="d.id!"
            :disabled="d.status !== 0"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="授权放行">
        <el-checkbox v-model="assignForm.override">
          证件已过期，带原因授权放行（只挂管理员）
        </el-checkbox>
      </el-form-item>
      <el-form-item v-if="assignForm.override" label="放行原因">
        <el-input v-model="assignForm.overrideReason" type="textarea" :rows="2" placeholder="必填：为什么要带着过期证件出车" />
        <div class="tip">
          留痕：原因、授权人、时间都会记到这趟任务上。车辆维修中、司机离职这类**硬门禁不可绕过**。
        </div>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="submitAssign" type="primary" :disabled="formLoading">确 定</el-button>
      <el-button @click="assignVisible = false">取 消</el-button>
    </template>
  </Dialog>

  <!-- 取消 -->
  <Dialog title="取消运输任务" v-model="cancelVisible" width="480px">
    <el-form label-width="90px">
      <el-form-item label="取消原因">
        <el-input v-model="cancelReason" type="textarea" :rows="3" placeholder="必填：为什么这趟活没跑" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="submitCancel" type="danger" :disabled="formLoading">确认取消</el-button>
      <el-button @click="cancelVisible = false">返 回</el-button>
    </template>
  </Dialog>

  <!-- 详情与时间线 -->
  <el-drawer v-model="detailVisible" title="运输任务详情" size="640px">
    <div v-if="detail" v-loading="detailLoading" class="detail">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="任务单号">{{ detail.taskNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="STATUS_TAG[detail.status!] || 'info'">{{ detail.statusName }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="车牌号">{{ detail.plateNo || '—' }}</el-descriptions-item>
        <el-descriptions-item label="司机">
          {{ detail.driverName || '—' }}{{ detail.driverMobile ? ' · ' + detail.driverMobile : '' }}
        </el-descriptions-item>
        <el-descriptions-item label="出发地">{{ detail.departureAddress || '—' }}</el-descriptions-item>
        <el-descriptions-item label="提货点">{{ detail.pickupAddress }}</el-descriptions-item>
        <el-descriptions-item label="提货联系人">{{ detail.pickupContactName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ detail.pickupContactPhone || '—' }}</el-descriptions-item>
        <el-descriptions-item label="时间窗" :span="2">
          {{ formatTime(detail.expectedStartTime) }} ~ {{ formatTime(detail.expectedEndTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="采购安排" :span="2">{{ detail.purchaseOrderNo || '未关联（直接上门收购）' }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.overrideReason" label="授权放行" :span="2">
          <el-tag type="warning" class="mr-5px">证件过期放行</el-tag>
          {{ detail.overrideReason }}（{{ formatTime(detail.overrideTime) }}）
        </el-descriptions-item>
        <el-descriptions-item v-if="detail.cancelReason" label="取消原因" :span="2">{{ detail.cancelReason }}</el-descriptions-item>
      </el-descriptions>

      <h4 class="section">运输时间线</h4>
      <el-timeline v-if="detail.nodes && detail.nodes.length">
        <el-timeline-item
          v-for="node in detail.nodes"
          :key="node.id"
          :timestamp="`发生 ${formatTime(node.nodeTime)} ／ 上报 ${formatTime(node.reportTime)}`"
          placement="top"
        >
          <el-card shadow="never">
            <div class="node__title">{{ node.nodeTypeName }}</div>
            <div class="node__line">位置：{{ node.location || '未记录' }}</div>
            <div class="node__line">上报人：{{ node.operatorName || '—' }}</div>
            <div v-if="node.photos && node.photos.length" class="node__line">凭证：{{ node.photos.length }} 张照片</div>
            <div v-else class="node__line node__line--warn">凭证：无照片</div>
          </el-card>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="还没有任何节点上报" />

      <el-alert
        v-if="detail.missingNodeNames && detail.missingNodeNames.length"
        type="warning"
        :closable="false"
        class="mt-10px"
        :title="`断点：${detail.missingNodeNames.join('、')} 尚未上报`"
      />

      <h4 class="section">补录节点（本期只支持「起运」）</h4>
      <el-form :model="nodeForm" label-width="90px" class="mt-10px">
        <el-form-item label="发生时间">
          <el-date-picker
            v-model="nodeForm.nodeTime"
            type="datetime"
            value-format="x"
            placeholder="事情实际发生的时刻"
            class="!w-100%"
          />
        </el-form-item>
        <el-form-item label="位置">
          <el-input v-model="nodeForm.location" placeholder="如：城东场站门口" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="nodeForm.remark" placeholder="选填" />
        </el-form-item>
        <div class="tip">照片由司机端上报时拍摄（后续票）；PC 代录只记时间与位置。发生时间与上报时间会分开留痕——补录晚到不代表业务倒序。</div>
        <el-button type="primary" class="mt-10px" @click="submitNode" v-hasPermi="['logistics:transport-node:report']">
          上报起运
        </el-button>
      </el-form>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { LogisticsTransportTaskApi, LogisticsTransportNodeApi, LogisticsTransportTaskVO } from '@/api/logistics/task'
import { LogisticsVehicleApi, LogisticsVehicleVO } from '@/api/logistics/vehicle'
import { LogisticsDriverApi, LogisticsDriverVO } from '@/api/logistics/driver'
import { formatDate } from '@/utils/formatTime'

defineOptions({ name: 'LogisticsTask' })

const STATUS_NAME: Record<number, string> = {
  0: '待分配',
  1: '已分配',
  2: '已接单',
  3: '执行中',
  4: '已完成',
  5: '已取消'
}
const STATUS_TAG: Record<number, 'success' | 'warning' | 'info' | 'danger'> = {
  0: 'info',
  1: 'warning',
  2: 'warning',
  3: 'warning',
  4: 'success',
  5: 'danger'
}

const { t } = useI18n()
const message = useMessage()

const loading = ref(true)
const list = ref<LogisticsTransportTaskVO[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  taskNo: undefined,
  plateNo: undefined,
  status: undefined,
  pickupAddress: undefined
})
const queryFormRef = ref()

const vehicleOptions = ref<LogisticsVehicleVO[]>([])
const driverOptions = ref<LogisticsDriverVO[]>([])

const getList = async () => {
  loading.value = true
  try {
    const data = await LogisticsTransportTaskApi.getTaskPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}
const loadOptions = async () => {
  const [vehicles, drivers] = await Promise.all([
    LogisticsVehicleApi.getVehiclePage({ pageNo: 1, pageSize: 100 }),
    LogisticsDriverApi.getDriverPage({ pageNo: 1, pageSize: 100 })
  ])
  vehicleOptions.value = vehicles.list
  driverOptions.value = drivers.list
}
const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}
const resetQuery = () => {
  queryFormRef.value.resetFields()
  handleQuery()
}

// ==================== 新建 / 派车 ====================
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()
const formData = ref<LogisticsTransportTaskVO>(buildEmpty())
const timeWindow = ref<[number, number] | undefined>()

function buildEmpty(): LogisticsTransportTaskVO {
  return {
    pickupAddress: undefined,
    departureAddress: undefined,
    pickupContactName: undefined,
    pickupContactPhone: undefined,
    vehicleId: undefined,
    driverId: undefined,
    purchaseOrderNo: undefined,
    remark: undefined
  }
}

const formRules = reactive({
  pickupAddress: [{ required: true, message: '提货点地址不能为空', trigger: 'blur' }]
})

const openForm = async (type: string, id?: number) => {
  dialogVisible.value = true
  formType.value = type
  dialogTitle.value = t('action.' + type)
  formData.value = buildEmpty()
  timeWindow.value = undefined
  formRef.value?.resetFields()
  await loadOptions()
  if (id) {
    formLoading.value = true
    try {
      const detail = await LogisticsTransportTaskApi.getTask(id)
      formData.value = detail
      if (detail.expectedStartTime && detail.expectedEndTime) {
        timeWindow.value = [detail.expectedStartTime as unknown as number, detail.expectedEndTime as unknown as number]
      }
    } finally {
      formLoading.value = false
    }
  }
}

const submitForm = async () => {
  await formRef.value.validate()
  if (timeWindow.value?.length === 2) {
    formData.value.expectedStartTime = timeWindow.value[0] as unknown as Date
    formData.value.expectedEndTime = timeWindow.value[1] as unknown as Date
  } else {
    formData.value.expectedStartTime = undefined
    formData.value.expectedEndTime = undefined
  }
  formLoading.value = true
  try {
    if (formType.value === 'create') {
      await LogisticsTransportTaskApi.createTask(formData.value)
      message.success('已派车')
    } else {
      await LogisticsTransportTaskApi.updateTask(formData.value)
      message.success(t('common.updateSuccess'))
    }
    dialogVisible.value = false
    await getList()
  } finally {
    formLoading.value = false
  }
}

const assignVisible = ref(false)
const assignForm = reactive<{
  id?: number
  vehicleId?: number
  driverId?: number
  override?: boolean
  overrideReason?: string
}>({})
const openAssign = async (row: LogisticsTransportTaskVO) => {
  assignVisible.value = true
  assignForm.id = row.id
  assignForm.vehicleId = undefined
  assignForm.driverId = undefined
  assignForm.override = false
  assignForm.overrideReason = undefined
  await loadOptions()
}
const submitAssign = async () => {
  if (!assignForm.vehicleId || !assignForm.driverId) {
    message.warning('车辆与司机都要选')
    return
  }
  if (assignForm.override && !assignForm.overrideReason) {
    message.warning('授权放行必须填原因')
    return
  }
  formLoading.value = true
  try {
    if (assignForm.override) {
      // 走授权放行：证件过期时的逃生门，留痕原因/授权人/时间
      await LogisticsTransportTaskApi.assignTaskWithOverride({
        id: assignForm.id!,
        vehicleId: assignForm.vehicleId,
        driverId: assignForm.driverId,
        overrideReason: assignForm.overrideReason!
      })
      message.success('已带原因授权放行')
    } else {
      await LogisticsTransportTaskApi.assignTask({
        id: assignForm.id!,
        vehicleId: assignForm.vehicleId,
        driverId: assignForm.driverId
      })
      message.success('已派车')
    }
    assignVisible.value = false
    await getList()
  } finally {
    formLoading.value = false
  }
}

const handleAccept = async (id: number) => {
  await LogisticsTransportTaskApi.acceptTask(id)
  message.success('已接单')
  await getList()
}
const handleComplete = async (id: number) => {
  await LogisticsTransportTaskApi.completeTask(id)
  message.success('已完成，车辆已放回车队')
  await getList()
}

const cancelVisible = ref(false)
const cancelReason = ref('')
const cancelId = ref<number>()
const openCancel = (row: LogisticsTransportTaskVO) => {
  cancelVisible.value = true
  cancelId.value = row.id
  cancelReason.value = ''
}
const submitCancel = async () => {
  if (!cancelReason.value) {
    message.warning('取消原因必填')
    return
  }
  formLoading.value = true
  try {
    await LogisticsTransportTaskApi.cancelTask({ id: cancelId.value!, cancelReason: cancelReason.value })
    message.success('已取消，车辆已放回车队')
    cancelVisible.value = false
    await getList()
  } finally {
    formLoading.value = false
  }
}

// ==================== 详情与时间线 ====================
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<LogisticsTransportTaskVO>()
const nodeForm = reactive<{ nodeTime?: number; location?: string; remark?: string }>({})

const openDetail = async (id: number) => {
  detailVisible.value = true
  detailLoading.value = true
  nodeForm.nodeTime = Date.now()
  nodeForm.location = undefined
  nodeForm.remark = undefined
  try {
    detail.value = await LogisticsTransportTaskApi.getTask(id)
  } finally {
    detailLoading.value = false
  }
}

const submitNode = async () => {
  if (!nodeForm.nodeTime) {
    message.warning('发生时间不能为空')
    return
  }
  formLoading.value = true
  try {
    await LogisticsTransportNodeApi.reportNode({
      taskId: detail.value!.id!,
      nodeType: 3, // 起运（本期只支持这一类）
      nodeTime: nodeForm.nodeTime as unknown as Date,
      location: nodeForm.location,
      remark: nodeForm.remark,
      // 幂等键：重复点击/断网重发不会落下两个节点
      clientRequestId: newClientRequestId()
    })
    message.success('已上报起运')
    await openDetail(detail.value!.id!)
    await getList()
  } finally {
    formLoading.value = false
  }
}

/**
 * 时间显示：接口给的是毫秒时间戳（本项目 LocalDateTime 的协议格式，前端一律 value-format="x"），
 * 空值给「—」，避免时间线上出现 Invalid Date。
 */
function formatTime(value?: Date | string | null): string {
  return value ? formatDate(new Date(value)) : '—'
}

/**
 * 幂等键：与现场端的做法一致（不引入 uuid 依赖），前缀区分来源端。
 * 服务端按「租户 + 请求号」唯一，重复提交返回既有节点。
 */
function newClientRequestId() {
  return `admin-${Date.now()}-${Math.floor(Math.random() * 1e6)}`
}

/** 初始化 **/
getList()
</script>

<style scoped lang="scss">
.tip {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.6;
}
.section {
  margin: 20px 0 10px;
  font-size: 14px;
  font-weight: 600;
}
.detail {
  padding: 0 4px;
}
.node__title {
  font-weight: 600;
}
.node__line {
  color: var(--el-text-color-regular);
  font-size: 13px;
  line-height: 1.8;
}
.node__line--warn {
  color: var(--el-color-warning);
}
</style>
