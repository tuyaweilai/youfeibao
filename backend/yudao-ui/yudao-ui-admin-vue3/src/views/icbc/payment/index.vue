<template>
  <!-- 发起付款：对预开票成功的收购生成企业支付页面 -->
  <ContentWrap title="发起付款（按收购单 / 开票合作方订单号）">
    <el-alert
      type="info"
      :closable="false"
      class="mb-10px"
      title="付款由平台代回收企业发起，生成的是工行『企业支付页面』。货款从回收企业账户经公对私结算直付到出售者本人银行卡，资金不经平台任何自有账户。只有预开票成功的收购才能付款。"
    />
    <el-form ref="applyFormRef" :model="applyForm" :rules="applyRules" label-width="150px">
      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item label="开票合作方订单号" prop="partnerOrderId">
            <el-input v-model="applyForm.partnerOrderId" placeholder="等于收购单号，如 ACQ..." clearable />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="本次付款金额(元)" prop="amount">
            <el-input-number v-model="applyForm.amount" :min="0" :precision="2" :controls="false" class="!w-full" placeholder="必须等于收购单金额" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="机构编码" prop="verifiedCode">
            <el-input v-model="applyForm.verifiedCode" placeholder="场景支付时必输" clearable />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item label="U盾ID" prop="ukeyId">
            <el-input v-model="applyForm.ukeyId" placeholder="场景支付时必输" clearable />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item>
        <el-button type="primary" :loading="applyLoading" @click="handleApply" v-hasPermi="['icbc:payment:create']">
          发起付款
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 查询支付状态 -->
  <ContentWrap title="查询支付状态">
    <el-form :inline="true" label-width="120px">
      <el-form-item label="合作方订单号">
        <el-input v-model="queryOrderId" placeholder="请输入" clearable class="!w-260px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery" v-hasPermi="['icbc:payment:query']">查询</el-button>
        <el-button v-if="status?.reInitiable" type="warning" plain :loading="applyLoading"
          @click="handleReInitiate" v-hasPermi="['icbc:payment:create']">
          重新发起
        </el-button>
        <el-button v-if="canViewReceipt" type="primary" plain @click="handleViewReceipt"
          v-hasPermi="['icbc:payment:query']">
          查看转账回单
        </el-button>
      </el-form-item>
    </el-form>

    <el-descriptions v-if="status" :column="2" border>
      <el-descriptions-item label="合作方订单号">{{ status.partnerOrderId }}</el-descriptions-item>
      <el-descriptions-item label="支付订单号">{{ status.orderNo }}</el-descriptions-item>
      <el-descriptions-item label="支付状态">
        <el-tag :type="statusTagType(status.paymentStatus)">{{ status.paymentStatusName || '-' }}</el-tag>
        <span v-if="status.payStatus" class="ml-5px text-12px text-gray-400">工行码 {{ status.payStatus }}</span>
      </el-descriptions-item>
      <el-descriptions-item label="应付金额(元)">{{ status.paymentAmount ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="实际到账(元)">{{ status.actuallyReceivedAmount ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="支付时间">{{ formatTime(status.paymentTime) }}</el-descriptions-item>
      <el-descriptions-item label="支付流水号">{{ status.paymentSerialNo || '-' }}</el-descriptions-item>
      <el-descriptions-item label="工行订单号">{{ status.icbcOrderNo || '-' }}</el-descriptions-item>
      <el-descriptions-item label="转账回单号">{{ status.receiptNo || '-' }}</el-descriptions-item>
      <el-descriptions-item label="回单归档时间">{{ formatTime(status.receiptTime) }}</el-descriptions-item>
      <el-descriptions-item label="错误码">{{ status.errorCode || '-' }}</el-descriptions-item>
      <el-descriptions-item label="错误信息">{{ status.errorMsg || '-' }}</el-descriptions-item>
    </el-descriptions>
  </ContentWrap>

  <!-- 转账回单 -->
  <el-dialog v-model="receiptVisible" title="转账回单" width="560px">
    <el-descriptions v-if="receipt" :column="1" border>
      <el-descriptions-item label="合作方订单号">{{ receipt.partnerOrderId }}</el-descriptions-item>
      <el-descriptions-item label="转账回单号">{{ receipt.receiptNo || '-' }}</el-descriptions-item>
      <el-descriptions-item label="回单归档时间">{{ formatTime(receipt.receiptTime) }}</el-descriptions-item>
      <el-descriptions-item label="支付流水号">{{ receipt.paymentSerialNo || '-' }}</el-descriptions-item>
      <el-descriptions-item label="应付金额(元)">{{ receipt.paymentAmount ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="实际到账(元)">{{ receipt.actuallyReceivedAmount ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="支付状态">{{ receipt.paymentStatusName || '-' }}</el-descriptions-item>
      <el-descriptions-item label="回单文件">
        <el-link v-if="receipt.receiptFileUrl" type="primary" :href="receipt.receiptFileUrl" target="_blank">
          下载回单
        </el-link>
        <span v-else class="text-gray-400">工行未提供回单文件，以回单号与流水为准</span>
      </el-descriptions-item>
    </el-descriptions>
  </el-dialog>
</template>

<script setup lang="ts">
import { PaymentApi, PaymentApplyVO, PaymentReceiptVO, PaymentStatusVO } from '@/api/icbc/payment'
import { formatDate } from '@/utils/formatTime'
import { openIcbcForm } from '../util'

defineOptions({ name: 'IcbcPayment' })

const message = useMessage()

const applyLoading = ref(false)
const applyFormRef = ref()
const applyForm = ref<PaymentApplyVO>({
  partnerOrderId: undefined,
  amount: undefined,
  verifiedCode: undefined,
  ukeyId: undefined
})
const applyRules = {
  partnerOrderId: [{ required: true, message: '开票合作方订单号不能为空', trigger: 'blur' }]
}

const handleApply = async () => {
  await applyFormRef.value.validate()
  applyLoading.value = true
  try {
    const res = await PaymentApi.applyPayment(applyForm.value)
    if (res.payPageHtml) {
      openIcbcForm(res.payPageHtml, '企业支付页面')
      message.success(res.duplicate ? res.message || '该笔支付正在处理中' : '已生成企业支付页面')
    } else {
      message.alert(res.message || res.errorMsg || '未返回支付页面')
    }
    if (res.partnerOrderId) {
      queryOrderId.value = res.partnerOrderId
      await handleQuery()
    }
  } finally {
    applyLoading.value = false
  }
}

const queryOrderId = ref('')
const status = ref<PaymentStatusVO>()

const handleQuery = async () => {
  if (!queryOrderId.value) {
    message.warning('请输入合作方订单号')
    return
  }
  status.value = await PaymentApi.queryPayment({ partnerOrderId: queryOrderId.value })
}

// 失败 / 冲正 / 退汇 / 部分成功等异常状态可重新发起
const handleReInitiate = async () => {
  applyForm.value.partnerOrderId = queryOrderId.value
  await handleApply()
}

const canViewReceipt = computed(() => status.value?.paymentStatus === 2)

const receiptVisible = ref(false)
const receipt = ref<PaymentReceiptVO>()
const handleViewReceipt = async () => {
  receipt.value = await PaymentApi.getReceipt({ partnerOrderId: queryOrderId.value })
  receiptVisible.value = true
}

const statusTagType = (s?: number) => {
  if (s === 2) return 'success'
  if (s === 9) return 'warning'
  if (s !== undefined && [3, 4, 5, 6, 7].includes(s)) return 'danger'
  return 'info'
}

const formatTime = (value?: number) => (value ? formatDate(new Date(value), 'YYYY-MM-DD HH:mm:ss') : '-')
</script>
