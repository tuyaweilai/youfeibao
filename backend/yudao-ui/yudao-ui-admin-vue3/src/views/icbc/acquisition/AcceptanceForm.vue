<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible" width="640px">
    <el-descriptions v-if="acquisition" :column="2" border size="small" class="mb-16px">
      <el-descriptions-item label="收购单号">{{ acquisition.acquisitionNo }}</el-descriptions-item>
      <el-descriptions-item label="出售者">{{ acquisition.sellerName }}</el-descriptions-item>
      <el-descriptions-item label="品类">{{ acquisition.categoryName }}</el-descriptions-item>
      <el-descriptions-item label="净重（过磅）">{{ acquisition.netWeight ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="结算重量（计价基准）">{{ acquisition.settlementWeight ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="含税单价">{{ acquisition.unitPrice ?? '-' }}</el-descriptions-item>
    </el-descriptions>

    <el-alert
      type="info"
      :closable="false"
      show-icon
      class="mb-16px"
      title="拒收部分（退回量 + 余货出场量）不进应付、不进正常库存，但单据与原因仍保留可追溯。"
    />

    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="130px" v-loading="formLoading">
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="接收量" prop="acceptedWeight">
            <el-input-number v-model="formData.acceptedWeight" :min="0" :precision="4" class="w-full" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="退回量" prop="rejectedWeight">
            <el-input-number v-model="formData.rejectedWeight" :min="0" :precision="4" class="w-full" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="余货出场量" prop="residualWeight">
            <el-input-number v-model="formData.residualWeight" :min="0" :precision="4" class="w-full" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="称量差异">
            <el-tag :type="diffTagType" size="large">{{ weightDiffText }}</el-tag>
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="拒收原因" prop="rejectReason">
        <el-input v-model="formData.rejectReason" type="textarea" :rows="2" placeholder="退回量大于 0 时必填（差异要有人解释）" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" placeholder="如：现场与出售者当面复磅确认" />
      </el-form-item>

      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="应付金额（扣拒收后）">
          <span class="payable">{{ payableAmountText }}</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-form>

    <template #footer>
      <el-button @click="submitForm" type="primary" :disabled="formLoading">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import { AcquisitionAcceptanceReqVO, AcquisitionApi, AcquisitionVO } from '@/api/icbc/acquisition'

defineOptions({ name: 'IcbcAcquisitionAcceptanceForm' })

const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('记录接收结论')
const formLoading = ref(false)
const formRef = ref()
const acquisition = ref<AcquisitionVO>()
const formData = ref<AcquisitionAcceptanceReqVO>({ id: 0, acceptedWeight: 0 })

const formRules = reactive({
  acceptedWeight: [{ required: true, message: '接收量不能为空（全部拒收请填 0）', trigger: 'change' }]
})

/** 实物量：接收量优先，否则净重（与后端 resolvePhysicalWeight 同一口径） */
const physicalWeight = computed(() => formData.value.acceptedWeight ?? acquisition.value?.netWeight)
const weightDiff = computed(() => {
  const physical = physicalWeight.value
  const settlement = acquisition.value?.settlementWeight
  if (physical === undefined || physical === null || settlement === undefined || settlement === null) return null
  return Number(physical) - Number(settlement)
})
const weightDiffText = computed(() => {
  if (weightDiff.value === null) return '—'
  return `${weightDiff.value}（${weightDiff.value >= 0 ? '实物多于计价' : '计价多于实物'}）`
})
const diffTagType = computed(() => {
  if (weightDiff.value === null || weightDiff.value === 0) return 'info'
  return weightDiff.value > 0 ? 'warning' : 'danger'
})

/** 应付金额 =（结算重量 − 退回量 − 余货出场量）× 单价 + 调整项 */
const payableAmountText = computed(() => {
  const settlement = acquisition.value?.settlementWeight
  const unitPrice = acquisition.value?.unitPrice
  if (settlement === undefined || settlement === null || unitPrice === undefined || unitPrice === null) return '—'
  const payableWeight = Math.max(0, Number(settlement) - Number(formData.value.rejectedWeight || 0) - Number(formData.value.residualWeight || 0))
  const adjustment = Number(acquisition.value?.adjustmentAmount || 0)
  return (payableWeight * Number(unitPrice) + adjustment).toFixed(2)
})

const open = async (id: number) => {
  dialogVisible.value = true
  formLoading.value = true
  try {
    acquisition.value = await AcquisitionApi.getAcquisition(id)
    formData.value = {
      id,
      // 默认按「全额接收」带出：现场只处理实际拒收 / 余货，没拒收就直接确认
      acceptedWeight: acquisition.value.settlementWeight ?? acquisition.value.netWeight ?? 0,
      rejectedWeight: acquisition.value.rejectedWeight ?? 0,
      residualWeight: acquisition.value.residualWeight ?? 0,
      rejectReason: acquisition.value.rejectReason,
      remark: undefined
    }
  } finally {
    formLoading.value = false
  }
}
defineExpose({ open })

const emit = defineEmits(['success'])
const submitForm = async () => {
  await formRef.value.validate()
  formLoading.value = true
  try {
    await AcquisitionApi.recordAcceptance(formData.value)
    message.success('已记录接收结论')
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}
</script>

<style scoped>
.payable {
  color: var(--el-color-primary);
  font-weight: 600;
}
</style>
