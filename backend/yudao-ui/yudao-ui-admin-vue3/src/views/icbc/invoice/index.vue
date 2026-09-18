<template>
  <!-- 开票申请（预下单） -->
  <ContentWrap title="开票申请（预下单）">
    <el-alert
      type="info"
      :closable="false"
      class="mb-10px"
      title="预下单只生成『自然人确认页面』，不产生发票。真正的票要等自然人确认并付款之后。"
    />
    <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item label="合作方订单ID" prop="outOrderId">
            <el-input v-model="form.outOrderId" placeholder="我方业务单号" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="付方编号" prop="outVendorId">
            <el-input v-model="form.outVendorId" placeholder="回收企业 / 子商户编号" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="收方编号" prop="outUserId">
            <el-input v-model="form.outUserId" placeholder="自然人外部用户编号" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item label="发票类型" prop="invoiceType">
            <el-select v-model="form.invoiceType" class="w-full">
              <el-option label="增值税普通发票" value="02" />
              <el-option label="增值税专用发票" value="01" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="收购类型" prop="specificElements">
            <el-select v-model="form.specificElements" class="w-full">
              <el-option label="报废产品收购" value="24" />
              <el-option label="农产品收购" value="16" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="价税合计" prop="orderAmount">
            <el-input-number v-model="form.orderAmount" :min="0.01" :precision="2" class="w-full" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-divider content-position="left">出售者（自然人）</el-divider>
      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item label="姓名" prop="naturalPersonName">
            <el-input v-model="form.naturalPersonName" placeholder="自然人姓名" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="身份证号" prop="cardNumber">
            <el-input v-model="form.cardNumber" placeholder="身份证号" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="电话" prop="sellerTelephone">
            <el-input v-model="form.sellerTelephone" placeholder="手机号" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="地址" prop="sellerAddress">
        <el-input v-model="form.sellerAddress" placeholder="自然人常用住址" />
      </el-form-item>

      <el-divider content-position="left">回收企业（付方）</el-divider>
      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item label="纳税人识别号" prop="taxpayerNo">
            <el-input v-model="form.taxpayerNo" placeholder="回收企业税号" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="纳税人名称" prop="taxpayerName">
            <el-input v-model="form.taxpayerName" placeholder="回收企业名称" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="区域代码" prop="areaCode">
            <el-input v-model="form.areaCode" placeholder="如 110000" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item label="开票人" prop="drawerName">
            <el-input v-model="form.drawerName" placeholder="开票员姓名" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="开票人证件号" prop="drawerCardNumber">
            <el-input v-model="form.drawerCardNumber" placeholder="开票员身份证号" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-divider content-position="left">商品明细</el-divider>
      <el-table :data="form.goodsInfo" border size="small" class="mb-10px">
        <el-table-column label="选择品类" width="150">
          <template #default="scope">
            <el-select
              v-model="scope.row.goodsConfigId"
              size="small"
              placeholder="选品类"
              class="w-full"
              @change="(id: number) => applyGoodsConfig(scope.row, id)"
            >
              <el-option v-for="g in goodsOptions" :key="g.id" :label="g.name" :value="g.id!" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="序号" width="70" align="center">
          <template #default="scope">
            <el-input v-model="scope.row.goodsSeqno" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="项目名称" min-width="140">
          <template #default="scope">
            <el-input v-model="scope.row.projectName" size="small" placeholder="如 废铁" />
          </template>
        </el-table-column>
        <el-table-column label="数量" width="110">
          <template #default="scope">
            <el-input-number v-model="scope.row.goodsNum" :min="0.0001" :precision="4" size="small" :controls="false" class="w-full" />
          </template>
        </el-table-column>
        <el-table-column label="含税单价" width="120">
          <template #default="scope">
            <el-input-number v-model="scope.row.price" :min="0.01" :precision="2" size="small" :controls="false" class="w-full" />
          </template>
        </el-table-column>
        <el-table-column label="金额" width="120">
          <template #default="scope">
            <el-input-number v-model="scope.row.goodsAmt" :min="0.01" :precision="2" size="small" :controls="false" class="w-full" />
          </template>
        </el-table-column>
        <el-table-column label="单位" width="80">
          <template #default="scope">
            <el-input v-model="scope.row.units" size="small" placeholder="吨" />
          </template>
        </el-table-column>
        <el-table-column label="税率" width="100">
          <template #default="scope">
            <el-input-number v-model="scope.row.taxRate" :min="0" :max="1" :precision="2" :step="0.01" size="small" :controls="false" class="w-full" />
          </template>
        </el-table-column>
        <el-table-column label="税收分类编码" min-width="170">
          <template #default="scope">
            <el-input v-model="scope.row.mergedCode" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="70" align="center" fixed="right">
          <template #default="scope">
            <el-button link type="danger" @click="removeGoods(scope.$index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-button plain @click="addGoods">+ 添加商品</el-button>

      <el-form-item class="mt-20px">
        <el-button type="primary" :loading="loading" @click="handleSubmit" v-hasPermi="['icbc:invoice-order:create']">
          提交开票申请
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 预查询 -->
  <ContentWrap title="预查询">
    <el-form :inline="true" label-width="100px">
      <el-form-item label="合作方订单ID">
        <el-input v-model="queryOrderId" placeholder="请输入" clearable class="!w-260px" />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery" v-hasPermi="['icbc:invoice-order:query']">查询</el-button>
      </el-form-item>
    </el-form>
    <el-descriptions v-if="queryResult" :column="2" border>
      <el-descriptions-item label="订单号">{{ queryResult.orderNo }}</el-descriptions-item>
      <el-descriptions-item label="合作方订单ID">{{ queryResult.partnerOrderId }}</el-descriptions-item>
      <el-descriptions-item label="订单状态">{{ orderStatusLabel(queryResult.orderStatus) }}</el-descriptions-item>
      <el-descriptions-item label="开票状态">{{ stepLabel(INVOICE_STATUS, queryResult.invoiceStatus) }}</el-descriptions-item>
      <el-descriptions-item label="支付状态">{{ stepLabel(PAYMENT_STATUS, queryResult.paymentStatus) }}</el-descriptions-item>
      <el-descriptions-item label="缴税状态">{{ stepLabel(TAX_STATUS, queryResult.taxStatus) }}</el-descriptions-item>
      <el-descriptions-item label="发票号码">{{ queryResult.invoiceNo || '-' }}</el-descriptions-item>
      <el-descriptions-item label="发票金额">{{ queryResult.invoiceAmount }}</el-descriptions-item>
    </el-descriptions>
  </ContentWrap>
</template>

<script setup lang="ts">
import { GoodsInfoVO, InvoiceApi, InvoicePreOrderVO, InvoiceQueryRespVO } from '@/api/icbc/invoice'
import { GoodsConfigApi, GoodsConfigVO } from '@/api/icbc/goodsConfig'
import { openIcbcForm } from '../util'

defineOptions({ name: 'IcbcInvoice' })

const message = useMessage()

const ORDER_STATUS: Record<number, string> = {
  0: '待确认', 1: '已确认', 2: '已支付', 3: '已开票', 4: '已完成', 9: '已取消'
}
const INVOICE_STATUS: Record<number, string> = { 0: '未开票', 1: '开票中', 2: '开票成功', 3: '开票失败' }
const PAYMENT_STATUS: Record<number, string> = { 0: '未支付', 1: '支付中', 2: '支付成功', 3: '支付失败' }
const TAX_STATUS: Record<number, string> = { 0: '未缴税', 1: '缴税中', 2: '缴税成功', 3: '缴税失败' }
const orderStatusLabel = (s?: number) => (s !== undefined ? ORDER_STATUS[s] ?? s : '-')
const stepLabel = (map: Record<number, string>, s?: number) => (s !== undefined ? map[s] ?? s : '-')

function newGoods(seq: number): GoodsInfoVO {
  return {
    goodsConfigId: undefined,
    goodsSeqno: String(seq),
    projectName: undefined,
    goodsNum: undefined,
    price: undefined,
    goodsAmt: undefined,
    units: '吨',
    taxRate: 0.01,
    mergedCode: undefined
  }
}

// 编码配置：按品类带出单位 / 税率 / 税收分类编码
const goodsOptions = ref<GoodsConfigVO[]>([])
const applyGoodsConfig = (row: GoodsInfoVO, configId: number) => {
  const cfg = goodsOptions.value.find((g) => g.id === configId)
  if (!cfg) return
  row.projectName = cfg.name
  row.units = cfg.unit
  row.taxRate = cfg.taxRate
  row.mergedCode = cfg.mergedCode
}

function buildForm(): InvoicePreOrderVO {
  return {
    outOrderId: undefined,
    outVendorId: undefined,
    outUserId: undefined,
    invoiceType: '02',
    specificElements: '24',
    orderAmount: undefined,
    naturalPersonName: undefined,
    cardNumber: undefined,
    sellerAddress: undefined,
    sellerTelephone: undefined,
    taxpayerNo: undefined,
    taxpayerName: undefined,
    drawerName: undefined,
    drawerCardNumber: undefined,
    areaCode: '110000',
    goodsInfo: [newGoods(1)]
  }
}

const form = ref<InvoicePreOrderVO>(buildForm())
const formRef = ref()
const loading = ref(false)
const rules = reactive({
  outOrderId: [{ required: true, message: '合作方订单ID不能为空', trigger: 'blur' }],
  outVendorId: [{ required: true, message: '付方编号不能为空', trigger: 'blur' }],
  outUserId: [{ required: true, message: '收方编号不能为空', trigger: 'blur' }],
  orderAmount: [{ required: true, message: '价税合计不能为空', trigger: 'change' }],
  naturalPersonName: [{ required: true, message: '姓名不能为空', trigger: 'blur' }],
  cardNumber: [{ required: true, message: '身份证号不能为空', trigger: 'blur' }],
  sellerPhone: [{ required: false }],
  sellerTelephone: [{ required: true, message: '电话不能为空', trigger: 'blur' }],
  sellerAddress: [{ required: true, message: '地址不能为空', trigger: 'blur' }],
  taxpayerNo: [{ required: true, message: '纳税人识别号不能为空', trigger: 'blur' }],
  taxpayerName: [{ required: true, message: '纳税人名称不能为空', trigger: 'blur' }],
  drawerName: [{ required: true, message: '开票人不能为空', trigger: 'blur' }],
  drawerCardNumber: [{ required: true, message: '开票人证件号不能为空', trigger: 'blur' }]
})

const addGoods = () => {
  form.value.goodsInfo!.push(newGoods(form.value.goodsInfo!.length + 1))
}
const removeGoods = (index: number) => {
  form.value.goodsInfo!.splice(index, 1)
}

const handleSubmit = async () => {
  await formRef.value.validate()
  loading.value = true
  try {
    const origin = window.location.origin
    const payload: InvoicePreOrderVO = {
      ...form.value,
      goodsInfo: (form.value.goodsInfo || []).map((g) => ({
        goodsSeqno: g.goodsSeqno,
        projectName: g.projectName,
        goodsNum: g.goodsNum,
        goodsAmt: g.goodsAmt,
        price: g.price,
        units: g.units,
        taxRate: g.taxRate,
        mergedCode: g.mergedCode
      })),
      trxChannel: '01',
      asynFlag: '0',
      currency: '001',
      payJumpUrl: origin,
      invoiceNotifyUrl: origin,
      invoiceJumpUrl: origin,
      cardType: '111',
      drawerCardType: '111',
      iitProject: '1',
      mac: '00:00:00:00:00:00',
      taxRate: 0.01,
      buyerInvTypeCode: form.value.specificElements === '16' ? '01' : '04'
    }
    const res = await InvoiceApi.preOrder(payload)
    if (res.redirectUrl) {
      openIcbcForm(res.redirectUrl, '自然人确认页面')
      message.success(`预下单成功，订单号：${res.orderNo}`)
    } else {
      message.alert(res.returnMsg || '预下单未返回确认页面')
    }
  } finally {
    loading.value = false
  }
}

const queryOrderId = ref('')
const queryResult = ref<InvoiceQueryRespVO>()
const handleQuery = async () => {
  if (!queryOrderId.value) {
    message.warning('请输入合作方订单ID')
    return
  }
  queryResult.value = await InvoiceApi.query({ outOrderId: queryOrderId.value })
}

onMounted(async () => {
  try {
    goodsOptions.value = (await GoodsConfigApi.getEnabledList()) || []
  } catch {
    goodsOptions.value = []
  }
})
</script>
