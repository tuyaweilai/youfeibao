<template>
  <!-- 发起付款 -->
  <ContentWrap title="发起付款">
    <el-alert
      type="info"
      :closable="false"
      class="mb-10px"
      title="付款由平台代回收企业发起，生成的是工行『企业支付页面』。货款从回收企业账户直付到出售者本人银行卡，资金不经平台。"
    />
    <el-form ref="payFormRef" :model="payForm" :rules="payRules" label-width="120px">
      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item label="合作方编号" prop="appId">
            <el-input v-model="payForm.appId" placeholder="工行合作方 appId" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="合作方订单ID" prop="outOrderId">
            <el-input v-model="payForm.outOrderId" placeholder="对应开票申请的单号" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="付方编号" prop="outVendorId">
            <el-input v-model="payForm.outVendorId" placeholder="回收企业 / 子商户编号" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item label="收方编号" prop="outUserId">
            <el-input v-model="payForm.outUserId" placeholder="自然人外部用户编号" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="机构编码" prop="verifiedCode">
            <el-input v-model="payForm.verifiedCode" placeholder="场景支付时必输" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="U盾ID" prop="ukeyId">
            <el-input v-model="payForm.ukeyId" placeholder="场景支付时必输" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item>
        <el-button type="primary" :loading="payLoading" @click="handlePay" v-hasPermi="['icbc:payment:create']">
          发起付款
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 查询支付状态 -->
  <ContentWrap title="查询支付状态">
    <el-form :inline="true" label-width="100px">
      <el-form-item label="合作方订单ID">
        <el-input v-model="queryOrderId" placeholder="请输入" clearable class="!w-260px" />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery" v-hasPermi="['icbc:payment:query']">查询</el-button>
      </el-form-item>
    </el-form>
    <el-descriptions v-if="status" :column="2" border>
      <el-descriptions-item label="合作方订单ID">{{ status.outOrderId }}</el-descriptions-item>
      <el-descriptions-item label="工行订单号">{{ status.icbcOrderNo }}</el-descriptions-item>
      <el-descriptions-item label="支付状态">
        <el-tag :type="status.paymentStatus === 'SUCCESS' ? 'success' : 'info'">
          {{ status.paymentStatus || '-' }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="支付金额">{{ status.paymentAmount }}</el-descriptions-item>
      <el-descriptions-item label="支付时间">{{ status.paymentTime || '-' }}</el-descriptions-item>
      <el-descriptions-item label="支付流水号">{{ status.paymentSerialNo || '-' }}</el-descriptions-item>
      <el-descriptions-item label="返回码">{{ status.returnCode }}</el-descriptions-item>
      <el-descriptions-item label="返回消息">{{ status.returnMsg }}</el-descriptions-item>
      <el-descriptions-item label="错误码">{{ status.errorCode || '-' }}</el-descriptions-item>
      <el-descriptions-item label="错误信息">{{ status.errorMsg || '-' }}</el-descriptions-item>
    </el-descriptions>
  </ContentWrap>
</template>

<script setup lang="ts">
import { PaymentApi, PaymentStatusVO, PaymentVO } from '@/api/icbc/payment'
import { openIcbcForm } from '../util'

defineOptions({ name: 'IcbcPayment' })

const message = useMessage()

const payLoading = ref(false)
const payFormRef = ref()
const payForm = ref<PaymentVO>({
  appId: undefined,
  outOrderId: undefined,
  outVendorId: undefined,
  outUserId: undefined,
  verifiedCode: undefined,
  ukeyId: undefined
})
const payRules = reactive({
  appId: [{ required: true, message: '合作方编号不能为空', trigger: 'blur' }],
  outOrderId: [{ required: true, message: '合作方订单ID不能为空', trigger: 'blur' }]
})

const handlePay = async () => {
  await payFormRef.value.validate()
  payLoading.value = true
  try {
    const res = await PaymentApi.createPayment(payForm.value)
    if (res.redirectUrl) {
      openIcbcForm(res.redirectUrl)
      message.success('已生成企业支付页面')
    } else {
      message.alert(res.returnMsg || '未返回支付页面')
    }
  } finally {
    payLoading.value = false
  }
}

const queryOrderId = ref('')
const status = ref<PaymentStatusVO>()
const handleQuery = async () => {
  if (!queryOrderId.value) {
    message.warning('请输入合作方订单ID')
    return
  }
  status.value = await PaymentApi.queryPayment({ outOrderId: queryOrderId.value })
}
</script>
