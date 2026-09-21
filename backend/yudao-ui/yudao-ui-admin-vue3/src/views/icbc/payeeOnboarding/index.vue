<template>
  <ContentWrap>
    <!-- 回收现场：先带档，再建档 -->
    <el-form :inline="true" :model="queryParams" class="-mb-15px" label-width="88px">
      <el-form-item label="姓名">
        <el-input v-model="queryParams.name" placeholder="请输入姓名" clearable class="!w-180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="身份证号">
        <el-input v-model="queryParams.idCardNo" placeholder="扫码 / 输入身份证号" clearable class="!w-240px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="手机号">
        <el-input v-model="queryParams.mobile" placeholder="请输入手机号" clearable class="!w-180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" plain @click="handleReturningCustomer">
          <Icon icon="ep:user" class="mr-5px" /> 回头客带档
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="ID" align="center" prop="id" width="80" />
      <el-table-column label="姓名" align="center" prop="name" min-width="100" />
      <el-table-column label="身份证号" align="center" prop="idCardNo" min-width="180" />
      <el-table-column label="手机号" align="center" prop="mobile" width="130" />
      <el-table-column label="审核状态" align="center" prop="status" width="110">
        <template #default="scope">
          <el-tag :type="statusType(scope.row.status)">{{ statusLabel(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" fixed="right" width="120">
        <template #default="scope">
          <el-button link type="primary" @click="openOnboarding(scope.row.id)">建档</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </ContentWrap>

  <!-- 建档向导 -->
  <el-drawer v-model="drawerVisible" :title="`出售者建档 - ${overview.name || ''}`" size="720px">
    <div v-loading="drawerLoading">
      <!-- 状态卡 -->
      <el-alert
        :type="overview.invoiceEligible ? 'success' : 'warning'"
        :closable="false"
        class="mb-16px"
      >
        <template #title>
          <span v-if="overview.invoiceEligible">建档完成，该出售者可用于开票</span>
          <span v-else>暂不可开票：{{ overview.invoiceBlockReason || '建档未完成' }}</span>
        </template>
        <div class="mt-4px text-13px">
          实人认证：{{ overview.realNameStatusName || '未认证' }}
          <span v-if="overview.realNameMsg">（{{ overview.realNameMsg }}）</span>
          ｜ 收方入驻：{{ overview.onboardingStateName || '未开始' }}
          <span v-if="overview.nextStep">（下一步：{{ overview.nextStep }}）</span>
        </div>
      </el-alert>

      <!-- 1. 实人认证 -->
      <el-card shadow="never" class="mb-16px">
        <template #header>1. 实人认证</template>
        <el-space wrap>
          <el-button type="primary" @click="handleStartRealName">发起实人认证</el-button>
          <el-button @click="handleSyncRealName">查询结果</el-button>
          <el-tag :type="overview.realNameStatus === 2 ? 'success' : 'info'">
            {{ overview.realNameStatusName || '未认证' }}
          </el-tag>
        </el-space>
      </el-card>

      <!-- 2. 收方入驻 -->
      <el-card shadow="never" class="mb-16px">
        <template #header>2. 收方入驻（绑定出售者本人银行卡）</template>
        <el-form :model="onboardingForm" label-width="110px">
          <el-form-item label="银行卡号">
            <el-input v-model="payee.bankCardNo" disabled />
          </el-form-item>
          <el-form-item label="证件签发日期">
            <el-input v-model="onboardingForm.idSignDate" placeholder="yyyy-MM-dd" />
          </el-form-item>
          <el-form-item label="证件截止日期">
            <el-input v-model="onboardingForm.idValidityPeriod" placeholder="永久有效填 9999-12-30" />
          </el-form-item>
          <el-form-item label="是否我行卡">
            <el-radio-group v-model="onboardingForm.accountCode">
              <el-radio value="1">工行卡</el-radio>
              <el-radio value="0">他行卡</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="开户银行">
            <el-input v-model="onboardingForm.bankName" placeholder="银行卡识别结果" />
          </el-form-item>
          <el-form-item label="开户支行">
            <el-input v-model="onboardingForm.bankBranch" placeholder="银行卡识别结果" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSubmitOnboarding">发起收方入驻</el-button>
            <el-button @click="handleSyncOnboarding">查询结果</el-button>
          </el-form-item>
          <el-alert type="info" :closable="false" show-icon
            title="收方入驻走工行数据接口，直接受理、无页面；受理后状态为「审核中」，结论以审核通知或主动查询为准。" />
        </el-form>
        <el-divider v-if="overview.onboardingState && !overview.invoiceEligible" />
        <el-form v-if="overview.onboardingState && !overview.invoiceEligible" :model="leadForm" inline>
          <el-form-item label="留联系方式">
            <el-input v-model="leadForm.mobile" placeholder="手机号" class="!w-200px" />
            <el-input v-model="leadForm.remark" placeholder="备注" class="!w-240px ml-8px" />
            <el-button type="warning" class="ml-8px" @click="handleLeaveContact">提交</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 3. 框架收购协议 -->
      <el-card shadow="never" class="mb-16px">
        <template #header>3. 框架收购协议（每个出售者一份，长期有效）</template>
        <el-descriptions v-if="overview.frameworkAgreement" :column="1" border size="small">
          <el-descriptions-item label="协议编号">{{ overview.frameworkAgreement.agreementNo }}</el-descriptions-item>
          <el-descriptions-item label="名称">{{ overview.frameworkAgreement.productName }}</el-descriptions-item>
          <el-descriptions-item label="数量">{{ overview.frameworkAgreement.quantity }}</el-descriptions-item>
          <el-descriptions-item label="规格">{{ overview.frameworkAgreement.specification }}</el-descriptions-item>
          <el-descriptions-item label="回收期次">{{ overview.frameworkAgreement.recyclePeriod }}</el-descriptions-item>
          <el-descriptions-item label="结算方式">{{ overview.frameworkAgreement.settlementMethod }}</el-descriptions-item>
        </el-descriptions>
        <el-form :model="agreementForm" label-width="90px" class="mt-8px">
          <el-form-item label="名称">
            <el-input v-model="agreementForm.productName" placeholder="如：废钢" />
          </el-form-item>
          <el-form-item label="数量">
            <el-input v-model="agreementForm.quantity" placeholder="如：5 吨" />
          </el-form-item>
          <el-form-item label="规格">
            <el-input v-model="agreementForm.specification" placeholder="如：重型废钢" />
          </el-form-item>
          <el-form-item label="回收期次">
            <el-input v-model="agreementForm.recyclePeriod" placeholder="如：2026 年 9 月第 1 期" />
          </el-form-item>
          <el-form-item label="结算方式">
            <el-input v-model="agreementForm.settlementMethod" placeholder="如：银行转账，过磅后 3 日内结清" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSaveAgreement">
              {{ overview.frameworkAgreement ? '重签协议' : '签署协议' }}
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 4. 首次授权 -->
      <el-card shadow="never" class="mb-16px">
        <template #header>4. 首次反向开票与代办税费授权</template>
        <el-form :model="authForm" label-width="150px">
          <el-form-item label="授权反向开票">
            <el-switch v-model="authForm.reverseInvoiceAuthorized" />
          </el-form-item>
          <el-form-item label="授权代办税费">
            <el-switch v-model="authForm.taxAgencyAuthorized" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleAuthorize">保存授权</el-button>
            <el-tag v-if="overview.authorization" class="ml-8px" type="success">
              已授权于 {{ formatTime(overview.authorization.authorizedAt) }}
            </el-tag>
          </el-form-item>
        </el-form>
      </el-card>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { PayeeApi, PayeeVO } from '@/api/icbc/payee'
import {
  OnboardingApi,
  SellerOnboardingVO,
  FrameworkAgreementVO,
  SellerAuthorizationVO
} from '@/api/icbc/onboarding'
import { openIcbcForm } from '../util'
import { formatDate } from '@/utils/formatTime'

/** 出售者建档 */
defineOptions({ name: 'IcbcPayeeOnboarding' })

const message = useMessage()

const loading = ref(true)
const list = ref<PayeeVO[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  name: undefined,
  idCardNo: undefined,
  mobile: undefined
})

const drawerVisible = ref(false)
const drawerLoading = ref(false)
const payee = ref<PayeeVO>({})
const overview = ref<SellerOnboardingVO>({})
const onboardingForm = reactive({
  payeeId: undefined as number | undefined,
  idSignDate: '',
  idValidityPeriod: '',
  bankName: '',
  bankBranch: '',
  accountCode: '1'
})
const leadForm = reactive({ mobile: '', remark: '' })
const agreementForm = reactive<FrameworkAgreementVO>({})
const authForm = reactive<SellerAuthorizationVO>({
  reverseInvoiceAuthorized: false,
  taxAgencyAuthorized: false
})

const getList = async () => {
  loading.value = true
  try {
    const data = await PayeeApi.getPayeePage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}
const resetQuery = () => {
  queryParams.name = undefined
  queryParams.idCardNo = undefined
  queryParams.mobile = undefined
  handleQuery()
}

/** 回头客：扫身份证或用手机号带出既有档案，不重新登记 */
const handleReturningCustomer = async () => {
  if (!queryParams.idCardNo && !queryParams.mobile) {
    message.warning('请先输入身份证号或手机号')
    return
  }
  const found = await OnboardingApi.findReturningCustomer({
    idCardNo: queryParams.idCardNo,
    mobile: queryParams.mobile
  })
  if (!found) {
    message.info('未找到既有档案，请先新增出售者')
    return
  }
  openOnboarding(found.id)
}

const openOnboarding = async (payeeId: number) => {
  drawerVisible.value = true
  drawerLoading.value = true
  try {
    payee.value = await PayeeApi.getPayee(payeeId)
    onboardingForm.payeeId = payeeId
    onboardingForm.bankName = payee.value.bankName || ''
    onboardingForm.bankBranch = payee.value.bankBranch || ''
    leadForm.mobile = payee.value.mobile || ''
    agreementForm.payeeId = payeeId
    authForm.payeeId = payeeId
    await refreshOverview()
  } finally {
    drawerLoading.value = false
  }
}

const refreshOverview = async () => {
  overview.value = await OnboardingApi.getOnboarding(onboardingForm.payeeId!)
}

const handleStartRealName = async () => {
  const step = await OnboardingApi.startRealName(onboardingForm.payeeId!)
  openIcbcForm(step.formHtml, '实人认证')
  await refreshOverview()
}

const handleSyncRealName = async () => {
  overview.value = await OnboardingApi.syncRealName(onboardingForm.payeeId!)
  message.success('已刷新实人认证结果')
}

const handleSubmitOnboarding = async () => {
  // 数据接口直接受理：没有工行页面可开，返回的就是刷新后的建档总览
  overview.value = await OnboardingApi.submitOnboarding({ ...onboardingForm })
  message.success('已受理，等待工行审核结果')
}

const handleSyncOnboarding = async () => {
  overview.value = await OnboardingApi.syncOnboarding(onboardingForm.payeeId!)
  if (!overview.value.invoiceEligible && overview.value.nextStep) {
    message.warning(`下一步：${overview.value.nextStep}`)
  }
}

const handleLeaveContact = async () => {
  if (!leadForm.mobile) {
    message.warning('请填写联系方式')
    return
  }
  await OnboardingApi.leaveContactFallback({
    payeeId: onboardingForm.payeeId!,
    mobile: leadForm.mobile,
    remark: leadForm.remark
  })
  message.success('已留下联系方式，等待平台联系')
}

const handleSaveAgreement = async () => {
  await OnboardingApi.saveAgreement({ ...agreementForm, payeeId: onboardingForm.payeeId! })
  message.success('协议已保存')
  await refreshOverview()
}

const handleAuthorize = async () => {
  await OnboardingApi.authorize({ ...authForm, payeeId: onboardingForm.payeeId!, channel: 'ONSITE' })
  message.success('授权已留痕')
  await refreshOverview()
}

const statusMap: Record<number, { label: string; type: 'info' | 'success' | 'danger' }> = {
  0: { label: '待审核', type: 'info' },
  1: { label: '审核通过', type: 'success' },
  2: { label: '审核拒绝', type: 'danger' }
}
const statusLabel = (status?: number) =>
  status !== undefined && statusMap[status] ? statusMap[status].label : '未知'
const statusType = (status?: number): 'info' | 'success' | 'danger' =>
  status !== undefined && statusMap[status] ? statusMap[status].type : 'info'

/** 后端把 LocalDateTime 序列化为毫秒时间戳 */
const formatTime = (ts?: number) => (ts ? formatDate(new Date(ts)) : '')

onMounted(() => {
  getList()
})
</script>
