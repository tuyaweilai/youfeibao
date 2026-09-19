<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible">
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="120px" v-loading="formLoading">
      <template v-if="formType === 'create'">
        <el-form-item label="出售者" prop="payeeId">
          <el-select v-model="formData.payeeId" placeholder="从既有出售者档案选择" filterable class="w-full">
            <el-option
              v-for="item in payees"
              :key="item.id"
              :label="`${item.name}（${item.mobile || item.idCardNo}）`"
              :value="item.id!"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="品类" prop="goodsConfigId">
          <el-select v-model="formData.goodsConfigId" placeholder="选择品类，自动带出单位 / 税率 / 编码" filterable class="w-full" @change="applyCategory">
            <el-option
              v-for="item in goodsConfigs"
              :key="item.id"
              :label="`${item.name}（${item.unit} / ${item.mergedCode}）`"
              :value="item.id!"
            />
          </el-select>
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="规格" prop="specification">
              <el-input v-model="formData.specification" placeholder="如：重型" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结算方式" prop="settlementMethod">
              <el-input v-model="formData.settlementMethod" placeholder="如：过磅后 3 日内结清" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="数量" prop="quantity">
              <el-input-number v-model="formData.quantity" :min="0" :precision="4" class="w-full" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="含税单价" prop="unitPrice">
              <el-input-number v-model="formData.unitPrice" :min="0" :precision="2" class="w-full" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="金额" prop="amount">
              <el-input-number v-model="formData.amount" :min="0" :precision="2" class="w-full" placeholder="留空按数量×单价" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="毛重" prop="grossWeight">
              <el-input-number v-model="formData.grossWeight" :min="0" :precision="4" class="w-full" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="皮重" prop="tareWeight">
              <el-input-number v-model="formData.tareWeight" :min="0" :precision="4" class="w-full" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="净重" prop="netWeight">
              <el-input-number v-model="formData.netWeight" :min="0" :precision="4" class="w-full" placeholder="留空按毛重−皮重" />
            </el-form-item>
          </el-col>
        </el-row>
      </template>

      <el-form-item label="磅单号" prop="weightTicketNo">
        <el-input v-model="formData.weightTicketNo" placeholder="磅单编号" />
      </el-form-item>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="磅单识别车牌" prop="weightTicketPlateNo">
            <el-input v-model="formData.weightTicketPlateNo" placeholder="识别结果可人工修正" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="车辆识别车牌" prop="vehiclePlateNo">
            <el-input v-model="formData.vehiclePlateNo" placeholder="车头 / 车尾照片识别结果" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item v-if="formType === 'correct'" label="修正说明" prop="remark">
        <el-input v-model="formData.remark" type="textarea" :rows="2" placeholder="说明为什么修正" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="submitForm" type="primary" :disabled="formLoading">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>

  <!-- 登记后的额度余量提示：额度是自然人跨租户累计的，现场不看就没人看得到 -->
  <Dialog v-model="quotaVisible" title="该出售者的额度余量" width="520px">
    <el-alert
      v-if="quotaResp"
      :type="quotaResp.quotaPassed === false ? 'error' : 'success'"
      :closable="false"
      :title="quotaResp.quotaMessage"
      class="mb-10px"
    />
    <el-descriptions v-if="quotaResp" :column="2" border size="small">
      <el-descriptions-item label="收购单号">{{ quotaResp.acquisitionNo }}</el-descriptions-item>
      <el-descriptions-item label="12 个月上限(元)">{{ quotaResp.quotaCapAmount }}</el-descriptions-item>
      <el-descriptions-item label="已用额度(元)">{{ quotaResp.quotaUsedAmount }}</el-descriptions-item>
      <el-descriptions-item label="剩余额度(元)">{{ quotaResp.quotaRemainingAmount }}</el-descriptions-item>
    </el-descriptions>
    <p v-if="quotaResp && quotaResp.quotaPassed === false" class="quota-warn">
      这一笔登记已落库，但额度已超：开票申请会被拒，请引导出售者办理经营主体登记。
    </p>
    <p v-else-if="quotaResp && quotaResp.monthlyOverExempt" class="quota-tip">
      本月销售额已超过 10 万元免征线，需按时代办申报缴款。
    </p>
    <template #footer>
      <el-button type="primary" @click="quotaVisible = false">知道了</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import { AcquisitionApi, AcquisitionCreateRespVO, AcquisitionVO } from '@/api/icbc/acquisition'
import { PayeeApi, PayeeVO } from '@/api/icbc/payee'
import { GoodsConfigApi, GoodsConfigVO } from '@/api/icbc/goodsConfig'

defineOptions({ name: 'IcbcAcquisitionForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formData = ref<AcquisitionVO>(buildEmpty())
const formRef = ref()
const payees = ref<PayeeVO[]>([])
const goodsConfigs = ref<GoodsConfigVO[]>([])
const quotaVisible = ref(false)
const quotaResp = ref<AcquisitionCreateRespVO>()

function buildEmpty(): AcquisitionVO {
  return {
    id: undefined,
    payeeId: undefined,
    goodsConfigId: undefined,
    specification: undefined,
    quantity: undefined,
    unitPrice: undefined,
    amount: undefined,
    grossWeight: undefined,
    tareWeight: undefined,
    netWeight: undefined,
    weightTicketNo: undefined,
    weightTicketPlateNo: undefined,
    vehiclePlateNo: undefined,
    tradeAddress: undefined,
    tradeTime: Date.now(),
    settlementMethod: undefined,
    source: 'ONLINE',
    remark: undefined
  }
}

const formRules = reactive({
  payeeId: [{ required: true, message: '出售者不能为空', trigger: 'change' }],
  goodsConfigId: [{ required: true, message: '品类不能为空', trigger: 'change' }],
  quantity: [{ required: true, message: '数量不能为空', trigger: 'blur' }],
  weightTicketNo: [{ required: true, message: '磅单不能为空', trigger: 'blur' }]
})

const applyCategory = (id?: number) => {
  const config = goodsConfigs.value.find((item) => item.id === Number(id))
  if (!config) return
  formData.value.unit = config.unit
  formData.value.taxRate = config.taxRate
  formData.value.mergedCode = config.mergedCode
}

const loadOptions = async () => {
  if (payees.value.length === 0) {
    const data = await PayeeApi.getPayeePage({ pageNo: 1, pageSize: 100 })
    payees.value = data.list || []
  }
  if (goodsConfigs.value.length === 0) {
    goodsConfigs.value = (await GoodsConfigApi.getEnabledList()) || []
  }
}

const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  formType.value = type
  dialogTitle.value = type === 'create' ? '登记收购' : '修正识别结果'
  resetForm()
  await loadOptions()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await AcquisitionApi.getAcquisition(id)
    } finally {
      formLoading.value = false
    }
  }
}
defineExpose({ open })

const emit = defineEmits(['success'])
const submitForm = async () => {
  await formRef.value.validate()
  formLoading.value = true
  try {
    if (formType.value === 'create') {
      // 登记前后都要让收货员看到余量，不能等开票被拒才发现超了 500 万
      const resp = await AcquisitionApi.createAcquisition(formData.value)
      message.success(t('common.createSuccess'))
      quotaResp.value = resp
      quotaVisible.value = true
    } else {
      await AcquisitionApi.correctRecognition(formData.value)
      message.success(t('common.updateSuccess'))
    }
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}

const resetForm = () => {
  formData.value = buildEmpty()
  formRef.value?.resetFields()
}
</script>

<style scoped>
.quota-warn {
  margin-top: 10px;
  color: var(--el-color-danger);
  font-size: 13px;
}
.quota-tip {
  margin-top: 10px;
  color: var(--el-color-warning);
  font-size: 13px;
}
</style>
