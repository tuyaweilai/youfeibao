<template>
  <!-- 代办税费申报（#13）：按月清单、申报缴款、补缴与汇算清缴 -->
  <ContentWrap title="代办税费申报">
    <el-alert
      type="info"
      :closable="false"
      class="mb-10px"
      title="回收企业必须为出售者代办增值税及附加税费、个人所得税：次月申报期（15 日）前报送《代办税费报告表》《代办税费明细报告表》并缴款。逾期未缴，主管税务机关会暂停本企业的反向开票资格。月销售额超过 10 万元的出售者需单独列出。"
    />

    <el-alert
      v-for="warning in warnings"
      :key="warning.type + (warning.declarationId || '')"
      :type="warning.suspensionRisk ? 'error' : warning.type === 'MISSING_DATA' ? 'warning' : 'info'"
      :closable="false"
      class="mb-10px"
      :title="`【${warning.typeName}】${warning.message}`"
      :description="warning.nextAction"
    />

    <el-tabs v-model="activeTab">
      <!-- ========== 申报清单 ========== -->
      <el-tab-pane label="申报清单" name="declaration">
        <el-form :inline="true" label-width="80px">
          <el-form-item label="申报月">
            <el-date-picker v-model="periodMonth" type="month" value-format="YYYY-MM" class="!w-160px" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="generateLoading" @click="handleGenerate" v-hasPermi="['icbc:tax-declaration:manage']">
              生成 / 刷新清单
            </el-button>
            <el-button :disabled="!periodMonth" @click="handlePrecheck">检查缺项</el-button>
            <el-button @click="handleDeclarationQuery"><Icon icon="ep:search" class="mr-5px" />刷新列表</el-button>
          </el-form-item>
        </el-form>

        <el-table v-loading="declarationLoading" :data="declarationList" :stripe="true">
          <el-table-column label="申报月" prop="periodMonth" width="100" />
          <el-table-column label="状态" align="center" width="120">
            <template #default="{ row }">
              <el-tag :type="declarationTagType(row)">{{ row.statusName }}</el-tag>
              <el-tag v-if="row.overdue" type="danger" size="small" class="ml-1">已逾期</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="出售者" align="right" prop="sellerCount" width="90" />
          <el-table-column label="超 10 万(人)" align="right" prop="overExemptSellerCount" width="110" />
          <el-table-column label="净销售额(元)" align="right" prop="totalSalesAmount" width="130" />
          <el-table-column label="减按 1%(元)" align="right" prop="amountAtOnePercent" width="120" />
          <el-table-column label="放弃减按 3%(元)" align="right" prop="amountAtThreePercent" width="140" />
          <el-table-column label="应缴合计(元)" align="right" prop="totalTaxAmount" width="120" />
          <el-table-column label="截止日" align="center" prop="declarationDeadline" width="110" />
          <el-table-column label="数据" align="center" width="90">
            <template #default="{ row }">
              <el-tag v-if="row.dataReady" type="success" size="small">齐备</el-tag>
              <el-tag v-else type="warning" size="small">缺 {{ row.missingDataCount }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" align="center" width="260" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openDeclarationDetail(row)">明细</el-button>
              <el-button
                v-if="row.status === 0"
                link
                type="primary"
                @click="openDeclare(row)"
                v-hasPermi="['icbc:tax-declaration:manage']"
              >
                报送
              </el-button>
              <el-button
                v-if="row.status === 1"
                link
                type="success"
                @click="openPay(row)"
                v-hasPermi="['icbc:tax-declaration:manage']"
              >
                缴款归档
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <Pagination
          :total="declarationTotal"
          v-model:page="declarationQuery.pageNo"
          v-model:limit="declarationQuery.pageSize"
          @pagination="getDeclarationList"
        />
      </el-tab-pane>

      <!-- ========== 待补缴 ========== -->
      <el-tab-pane label="待补缴" name="supplement">
        <el-alert
          v-if="supplementSummary"
          :type="supplementSummary.pendingCount ? 'warning' : 'success'"
          :closable="false"
          class="mb-10px"
          :title="supplementSummary.message"
        />
        <el-form :inline="true" label-width="70px">
          <el-form-item label="状态下拉">
            <el-select v-model="supplementQuery.status" clearable placeholder="全部" class="!w-160px">
              <el-option label="待补缴" :value="0" />
              <el-option label="已补缴" :value="1" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button @click="getSupplementList"><Icon icon="ep:search" class="mr-5px" />搜索</el-button>
            <el-button type="primary" @click="openSupplementCreate" v-hasPermi="['icbc:tax-declaration:manage']">
              登记补缴
            </el-button>
          </el-form-item>
        </el-form>
        <el-table v-loading="supplementLoading" :data="supplementList" :stripe="true">
          <el-table-column label="补缴单号" prop="supplementNo" min-width="200" show-overflow-tooltip />
          <el-table-column label="申报月" prop="periodMonth" width="100" />
          <el-table-column label="出售者" prop="sellerName" width="100" />
          <el-table-column label="原因" prop="reason" min-width="180" show-overflow-tooltip />
          <el-table-column label="1% 部分(元)" align="right" prop="amountAtOnePercent" width="120" />
          <el-table-column label="3% 部分(元)" align="right" prop="amountAtThreePercent" width="120" />
          <el-table-column label="合计(元)" align="right" prop="amount" width="110" />
          <el-table-column label="状态" align="center" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'warning'">{{ row.statusName }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" align="center" width="100" fixed="right">
            <template #default="{ row }">
              <el-button
                v-if="row.status === 0"
                link
                type="success"
                @click="openSupplementPay(row)"
                v-hasPermi="['icbc:tax-supplement:manage']"
              >
                缴清
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <Pagination
          :total="supplementTotal"
          v-model:page="supplementQuery.pageNo"
          v-model:limit="supplementQuery.pageSize"
          @pagination="getSupplementList"
        />
      </el-tab-pane>

      <!-- ========== 汇算清缴 ========== -->
      <el-tab-pane label="汇算清缴" name="settlement">
        <el-alert
          type="info"
          :closable="false"
          class="mb-10px"
          title="出售者须在次年 3 月 31 日前自行汇算清缴经营所得。这里为当年有开票记录的出售者生成提醒与对账单；可生成免登录链接交给出售者，让他查看自己的开票与已缴税款信息。"
        />
        <el-form :inline="true" label-width="70px">
          <el-form-item label="纳税年度">
            <el-input-number v-model="settlementQuery.taxYear" :controls="false" class="!w-120px" />
          </el-form-item>
          <el-form-item>
            <el-button @click="getSettlementList"><Icon icon="ep:search" class="mr-5px" />搜索</el-button>
            <el-button type="primary" :loading="remindLoading" @click="handleRemind" v-hasPermi="['icbc:settlement:remind']">
              生成提醒
            </el-button>
          </el-form-item>
        </el-form>
        <el-table v-loading="settlementLoading" :data="settlementList" :stripe="true">
          <el-table-column label="出售者" prop="sellerName" width="110" />
          <el-table-column label="身份证" prop="idCardMasked" width="180" />
          <el-table-column label="年度" prop="taxYear" width="80" />
          <el-table-column label="开票(张)" align="right" prop="invoiceCount" width="90" />
          <el-table-column label="开票金额(元)" align="right" prop="invoicedAmount" width="130" />
          <el-table-column label="已预缴税费(元)" align="right" prop="paidTaxAmount" width="140" />
          <el-table-column label="其中个税(元)" align="right" prop="iitAmount" width="120" />
          <el-table-column label="截止日" align="center" prop="deadline" width="110" />
          <el-table-column label="状态" align="center" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'warning'">{{ row.statusName }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" align="center" width="230" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openStatement(row.payeeId, row.taxYear)">对账单</el-button>
              <el-button
                link
                type="primary"
                @click="handleStatementLink(row.payeeId)"
                v-hasPermi="['icbc:public-token:create']"
              >
                免登录链接
              </el-button>
              <el-button
                v-if="row.status === 0"
                link
                type="success"
                @click="openReminderHandle(row)"
                v-hasPermi="['icbc:settlement:remind']"
              >
                已提醒
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <Pagination
          :total="settlementTotal"
          v-model:page="settlementQuery.pageNo"
          v-model:limit="settlementQuery.pageSize"
          @pagination="getSettlementList"
        />
      </el-tab-pane>
    </el-tabs>
  </ContentWrap>

  <!-- 申报明细 -->
  <el-dialog v-model="detailVisible" title="代办税费申报明细" width="1000px">
    <el-descriptions v-if="currentDeclaration" :column="4" border size="small" class="mb-10px">
      <el-descriptions-item label="申报月">{{ currentDeclaration.periodMonth }}</el-descriptions-item>
      <el-descriptions-item label="状态">{{ currentDeclaration.statusName }}</el-descriptions-item>
      <el-descriptions-item label="免征线">月销售额 10 万元（跨租户合并判定）</el-descriptions-item>
      <el-descriptions-item label="缴款凭证">{{ currentDeclaration.voucherNo || '-' }}</el-descriptions-item>
    </el-descriptions>
    <el-alert
      v-if="currentDeclaration && !currentDeclaration.dataReady"
      type="warning"
      :closable="false"
      class="mb-10px"
      :title="`还有 ${currentDeclaration.missingDataCount} 项数据缺失，可点“检查缺项”查看怎么补`"
    />
    <el-table :data="currentDeclaration?.items || []" :stripe="true" max-height="460">
      <el-table-column label="出售者" prop="sellerName" width="100" />
      <el-table-column label="本租户净销售额(元)" align="right" prop="salesAmount" width="150" />
      <el-table-column label="跨租户月销售额(元)" align="right" prop="crossTenantMonthAmount" width="160" />
      <el-table-column label="超 10 万" align="center" width="90">
        <template #default="{ row }">
          <el-tag v-if="row.overExempt" type="warning" size="small">需单独申报</el-tag>
          <span v-else>否</span>
        </template>
      </el-table-column>
      <el-table-column label="减按 1%(元)" align="right" prop="amountAtOnePercent" width="120" />
      <el-table-column label="放弃减按 3%(元)" align="right" prop="amountAtThreePercent" width="140" />
      <el-table-column label="增值税(元)" align="right" prop="vatAmount" width="110" />
      <el-table-column label="附加税费(元)" align="right" prop="surchargeAmount" width="120" />
      <el-table-column label="个税(元)" align="right" prop="iitAmount" width="110" />
      <el-table-column label="合计(元)" align="right" prop="totalTaxAmount" width="110" />
    </el-table>
  </el-dialog>

  <!-- 缺项检查 -->
  <el-dialog v-model="precheckVisible" title="申报数据齐备性" width="720px">
    <el-alert
      v-if="precheck"
      :type="precheck.ready ? 'success' : 'warning'"
      :closable="false"
      class="mb-10px"
      :title="precheck.message"
    />
    <el-table :data="precheck?.missing || []" :stripe="true">
      <el-table-column label="缺项" prop="typeName" width="130" />
      <el-table-column label="说明" prop="message" min-width="240" />
      <el-table-column label="怎么补" prop="remedy" min-width="240" show-overflow-tooltip />
    </el-table>
  </el-dialog>

  <!-- 报送报告表 -->
  <el-dialog v-model="declareVisible" title="报送《代办税费报告表》" width="520px">
    <el-form :model="declareForm" label-width="90px">
      <el-form-item label="申报月">{{ declareForm.periodMonth }}</el-form-item>
      <el-form-item label="应缴合计(元)">{{ currentDeclaration?.totalTaxAmount }}</el-form-item>
      <el-form-item label="申报人">
        <el-input v-model="declareForm.declaredBy" placeholder="如：财务小李" />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="declareForm.remark" type="textarea" :rows="3" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="declareVisible = false">取消</el-button>
      <el-button type="primary" :loading="declareLoading" @click="submitDeclare">确认报送</el-button>
    </template>
  </el-dialog>

  <!-- 缴款归档 -->
  <el-dialog v-model="payVisible" title="缴款并归档凭证" width="520px">
    <el-form :model="payForm" label-width="110px">
      <el-form-item label="申报月">{{ payForm.periodMonth }}</el-form-item>
      <el-form-item label="应缴合计(元)">{{ currentDeclaration?.totalTaxAmount }}</el-form-item>
      <el-form-item label="实缴金额(元)">
        <el-input-number v-model="payForm.paidAmount" :precision="2" :controls="false" class="!w-200px" />
      </el-form-item>
      <el-form-item label="缴款时间">
        <el-date-picker
          v-model="payForm.paidAt"
          type="datetime"
          value-format="x"
          class="!w-220px"
        />
      </el-form-item>
      <el-form-item label="缴纳方式">
        <el-input v-model="payForm.paymentMethod" placeholder="如：电子税务局批量扣款" />
      </el-form-item>
      <el-form-item label="凭证号">
        <el-input v-model="payForm.voucherNo" />
      </el-form-item>
      <el-form-item label="凭证文件地址">
        <el-input v-model="payForm.voucherFileUrl" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="payVisible = false">取消</el-button>
      <el-button type="primary" :loading="payLoading" @click="submitPay">确认缴款</el-button>
    </template>
  </el-dialog>

  <!-- 登记补缴 -->
  <el-dialog v-model="supplementCreateVisible" title="登记需补缴税费" width="560px">
    <el-form :model="supplementForm" label-width="130px">
      <el-form-item label="所属申报月">
        <el-date-picker v-model="supplementForm.periodMonth" type="month" value-format="YYYY-MM" class="!w-160px" />
      </el-form-item>
      <el-form-item label="关联申报单编号">
        <el-input-number v-model="supplementForm.declarationId" :controls="false" class="!w-200px" />
      </el-form-item>
      <el-form-item label="出售者档案编号">
        <el-input-number v-model="supplementForm.payeeId" :controls="false" class="!w-200px" />
      </el-form-item>
      <el-form-item label="补缴原因">
        <el-input v-model="supplementForm.reason" type="textarea" :rows="2" />
      </el-form-item>
      <el-form-item label="减按 1% 部分(元)">
        <el-input-number v-model="supplementForm.amountAtOnePercent" :precision="2" :controls="false" class="!w-200px" />
      </el-form-item>
      <el-form-item label="放弃减按 3% 部分(元)">
        <el-input-number v-model="supplementForm.amountAtThreePercent" :precision="2" :controls="false" class="!w-200px" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="supplementCreateVisible = false">取消</el-button>
      <el-button type="primary" :loading="supplementCreateLoading" @click="submitSupplementCreate">确定</el-button>
    </template>
  </el-dialog>

  <!-- 缴清补缴 -->
  <el-dialog v-model="supplementPayVisible" title="缴清补缴" width="520px">
    <el-form :model="supplementPayForm" label-width="110px">
      <el-form-item label="应补缴(元)">{{ currentSupplement?.amount }}</el-form-item>
      <el-form-item label="实缴金额(元)">
        <el-input-number v-model="supplementPayForm.paidAmount" :precision="2" :controls="false" class="!w-200px" />
      </el-form-item>
      <el-form-item label="缴款时间">
        <el-date-picker v-model="supplementPayForm.paidAt" type="datetime" value-format="x" class="!w-220px" />
      </el-form-item>
      <el-form-item label="凭证号">
        <el-input v-model="supplementPayForm.voucherNo" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="supplementPayVisible = false">取消</el-button>
      <el-button type="primary" :loading="supplementPayLoading" @click="submitSupplementPay">确认缴清</el-button>
    </template>
  </el-dialog>

  <!-- 汇算清缴对账单 -->
  <el-dialog v-model="statementVisible" title="汇算清缴对账单" width="720px">
    <template v-if="statement">
      <el-alert :type="statement.overdue ? 'error' : 'info'" :closable="false" class="mb-10px" :title="statement.message" />
      <el-descriptions :column="3" border size="small" class="mb-10px">
        <el-descriptions-item label="出售者">{{ statement.sellerName }}</el-descriptions-item>
        <el-descriptions-item label="身份证">{{ statement.idCardMasked }}</el-descriptions-item>
        <el-descriptions-item label="年度">{{ statement.taxYear }}</el-descriptions-item>
        <el-descriptions-item label="开票张数">{{ statement.invoiceCount }}</el-descriptions-item>
        <el-descriptions-item label="开票金额(元)">{{ statement.invoicedAmount }}</el-descriptions-item>
        <el-descriptions-item label="已预缴税费(元)">{{ statement.paidTaxAmount }}</el-descriptions-item>
        <el-descriptions-item label="其中个税(元)">{{ statement.iitAmount }}</el-descriptions-item>
        <el-descriptions-item label="汇算截止日">{{ statement.deadline }}</el-descriptions-item>
        <el-descriptions-item label="剩余天数">{{ statement.daysLeft }}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="statement.months || []" :stripe="true" size="small">
        <el-table-column label="月份" prop="month" width="100" />
        <el-table-column label="开票(张)" align="right" prop="invoiceCount" />
        <el-table-column label="开票金额(元)" align="right" prop="invoicedAmount" />
        <el-table-column label="已预缴税费(元)" align="right" prop="paidTaxAmount" />
        <el-table-column label="其中个税(元)" align="right" prop="iitAmount" />
      </el-table>
    </template>
  </el-dialog>

  <!-- 汇算清缴免登录链接 -->
  <el-dialog v-model="linkVisible" title="汇算清缴对账链接" width="480px">
    <el-input v-model="statementLink" readonly />
    <p class="link-tip">发给出售者，他打开即可查看自己的开票与已缴税款，不需要注册。</p>
  </el-dialog>

  <!-- 标记已提醒 -->
  <el-dialog v-model="reminderHandleVisible" title="标记汇算清缴提醒" width="520px">
    <el-form :model="reminderHandleForm" label-width="90px">
      <el-form-item label="出售者">{{ currentReminder?.sellerName }}</el-form-item>
      <el-form-item label="处理说明">
        <el-input v-model="reminderHandleForm.handleRemark" type="textarea" :rows="3" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="reminderHandleVisible = false">取消</el-button>
      <el-button type="primary" :loading="reminderHandleLoading" @click="submitReminderHandle">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { PublicTokenApi, buildPublicUrl } from '@/api/icbc/publicToken'
import {
  SettlementApi,
  SettlementReminderVO,
  SellerSettlementStatementVO,
  TaxDeclarationApi,
  TaxDeclarationPrecheckVO,
  TaxDeclarationVO,
  TaxDeclarationWarningVO,
  TaxSupplementApi,
  TaxSupplementSummaryVO,
  TaxSupplementVO
} from '@/api/icbc/tax'

/** 代办税费申报（#13） */
defineOptions({ name: 'IcbcTax' })

const message = useMessage()

const monthToStr = (date: Date) => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`
const lastMonth = () => {
  const date = new Date()
  date.setMonth(date.getMonth() - 1)
  return monthToStr(date)
}

const activeTab = ref('declaration')
const periodMonth = ref(lastMonth())

// ==================== 申报清单 ====================

const warnings = ref<TaxDeclarationWarningVO[]>([])
const declarationLoading = ref(false)
const declarationList = ref<TaxDeclarationVO[]>([])
const declarationTotal = ref(0)
const declarationQuery = reactive({ pageNo: 1, pageSize: 10, periodMonth: undefined })
const generateLoading = ref(false)

const declarationTagType = (row: TaxDeclarationVO) => {
  if (row.status === 2) return 'success'
  if (row.status === 1) return 'warning'
  return 'info'
}

const getDeclarationList = async () => {
  declarationLoading.value = true
  try {
    const data = await TaxDeclarationApi.getDeclarationPage(declarationQuery)
    declarationList.value = data.list
    declarationTotal.value = data.total
  } finally {
    declarationLoading.value = false
  }
}
const handleDeclarationQuery = () => {
  declarationQuery.pageNo = 1
  getDeclarationList()
}
const loadWarnings = async () => {
  warnings.value = await TaxDeclarationApi.getWarnings()
}

const handleGenerate = async () => {
  if (!periodMonth.value) {
    message.warning('请选择申报月')
    return
  }
  generateLoading.value = true
  try {
    const declaration = await TaxDeclarationApi.generate(periodMonth.value)
    message.success(declaration.message || '申报清单已生成')
    await Promise.all([getDeclarationList(), loadWarnings()])
  } finally {
    generateLoading.value = false
  }
}

const detailVisible = ref(false)
const currentDeclaration = ref<TaxDeclarationVO>()
const openDeclarationDetail = async (row: TaxDeclarationVO) => {
  currentDeclaration.value = await TaxDeclarationApi.getDeclaration(row.periodMonth!)
  detailVisible.value = true
}

const precheckVisible = ref(false)
const precheck = ref<TaxDeclarationPrecheckVO>()
const handlePrecheck = async () => {
  precheck.value = await TaxDeclarationApi.precheck(periodMonth.value)
  precheckVisible.value = true
}

const declareVisible = ref(false)
const declareLoading = ref(false)
const declareForm = reactive({ periodMonth: '', declaredBy: '', remark: '' })
const openDeclare = (row: TaxDeclarationVO) => {
  currentDeclaration.value = row
  declareForm.periodMonth = row.periodMonth!
  declareForm.declaredBy = ''
  declareForm.remark = ''
  declareVisible.value = true
}
const submitDeclare = async () => {
  declareLoading.value = true
  try {
    const declaration = await TaxDeclarationApi.declare(declareForm)
    message.success(declaration.message || '已报送')
    declareVisible.value = false
    await Promise.all([getDeclarationList(), loadWarnings()])
  } finally {
    declareLoading.value = false
  }
}

const payVisible = ref(false)
const payLoading = ref(false)
const payForm = reactive({
  periodMonth: '',
  paidAmount: undefined as number | undefined,
  paidAt: undefined as number | undefined,
  paymentMethod: '电子税务局批量扣款',
  voucherNo: '',
  voucherFileUrl: ''
})
const openPay = (row: TaxDeclarationVO) => {
  currentDeclaration.value = row
  payForm.periodMonth = row.periodMonth!
  payForm.paidAmount = row.totalTaxAmount
  payForm.paidAt = Date.now()
  payForm.voucherNo = ''
  payForm.voucherFileUrl = ''
  payVisible.value = true
}
const submitPay = async () => {
  payLoading.value = true
  try {
    const declaration = await TaxDeclarationApi.pay({ ...payForm })
    message.success(declaration.message || '缴款成功')
    payVisible.value = false
    await Promise.all([getDeclarationList(), loadWarnings()])
  } finally {
    payLoading.value = false
  }
}

// ==================== 待补缴 ====================

const supplementLoading = ref(false)
const supplementList = ref<TaxSupplementVO[]>([])
const supplementTotal = ref(0)
const supplementSummary = ref<TaxSupplementSummaryVO>()
const supplementQuery = reactive({ pageNo: 1, pageSize: 10, status: undefined })

const getSupplementList = async () => {
  supplementLoading.value = true
  try {
    const [page, summary] = await Promise.all([
      TaxSupplementApi.getPage(supplementQuery),
      TaxSupplementApi.getSummary()
    ])
    supplementList.value = page.list
    supplementTotal.value = page.total
    supplementSummary.value = summary
  } finally {
    supplementLoading.value = false
  }
}

const supplementCreateVisible = ref(false)
const supplementCreateLoading = ref(false)
const supplementForm = reactive({
  periodMonth: lastMonth(),
  declarationId: undefined as number | undefined,
  payeeId: undefined as number | undefined,
  reason: '',
  amountAtOnePercent: 0,
  amountAtThreePercent: 0
})
const openSupplementCreate = () => {
  supplementForm.periodMonth = lastMonth()
  supplementForm.declarationId = undefined
  supplementForm.payeeId = undefined
  supplementForm.reason = ''
  supplementForm.amountAtOnePercent = 0
  supplementForm.amountAtThreePercent = 0
  supplementCreateVisible.value = true
}
const submitSupplementCreate = async () => {
  supplementCreateLoading.value = true
  try {
    await TaxSupplementApi.create({ ...supplementForm })
    message.success('已登记补缴')
    supplementCreateVisible.value = false
    await getSupplementList()
  } finally {
    supplementCreateLoading.value = false
  }
}

const supplementPayVisible = ref(false)
const supplementPayLoading = ref(false)
const currentSupplement = ref<TaxSupplementVO>()
const supplementPayForm = reactive({ id: 0, paidAmount: undefined as number | undefined, paidAt: undefined as number | undefined, voucherNo: '' })
const openSupplementPay = (row: TaxSupplementVO) => {
  currentSupplement.value = row
  supplementPayForm.id = row.id!
  supplementPayForm.paidAmount = row.amount
  supplementPayForm.paidAt = Date.now()
  supplementPayForm.voucherNo = ''
  supplementPayVisible.value = true
}
const submitSupplementPay = async () => {
  supplementPayLoading.value = true
  try {
    await TaxSupplementApi.pay({ ...supplementPayForm })
    message.success('补缴已缴清')
    supplementPayVisible.value = false
    await getSupplementList()
  } finally {
    supplementPayLoading.value = false
  }
}

// ==================== 汇算清缴 ====================

const settlementLoading = ref(false)
const settlementList = ref<SettlementReminderVO[]>([])
const settlementTotal = ref(0)
const settlementQuery = reactive({ pageNo: 1, pageSize: 10, taxYear: new Date().getFullYear() })

const getSettlementList = async () => {
  settlementLoading.value = true
  try {
    const data = await SettlementApi.getPage(settlementQuery)
    settlementList.value = data.list
    settlementTotal.value = data.total
  } finally {
    settlementLoading.value = false
  }
}

const remindLoading = ref(false)
const handleRemind = async () => {
  remindLoading.value = true
  try {
    const created = await SettlementApi.remind(settlementQuery.taxYear)
    message.success(`新增 ${created} 条汇算清缴提醒`)
    await getSettlementList()
  } finally {
    remindLoading.value = false
  }
}

const statementVisible = ref(false)
const statement = ref<SellerSettlementStatementVO>()
const openStatement = async (payeeId?: number, taxYear?: number) => {
  if (!payeeId) return
  statement.value = await SettlementApi.getStatement(payeeId, taxYear)
  statementVisible.value = true
}

const linkVisible = ref(false)
const statementLink = ref('')
const handleStatementLink = async (payeeId?: number) => {
  if (!payeeId) return
  const token = await PublicTokenApi.create({ purpose: 'SETTLEMENT_STATEMENT', payeeId })
  statementLink.value = buildPublicUrl('settlement', token.token!)
  linkVisible.value = true
}

const reminderHandleVisible = ref(false)
const reminderHandleLoading = ref(false)
const currentReminder = ref<SettlementReminderVO>()
const reminderHandleForm = reactive({ id: 0, status: 1, handleRemark: '' })
const openReminderHandle = (row: SettlementReminderVO) => {
  currentReminder.value = row
  reminderHandleForm.id = row.id!
  reminderHandleForm.status = 1
  reminderHandleForm.handleRemark = ''
  reminderHandleVisible.value = true
}
const submitReminderHandle = async () => {
  reminderHandleLoading.value = true
  try {
    await SettlementApi.handle({ ...reminderHandleForm })
    message.success('已标记')
    reminderHandleVisible.value = false
    await getSettlementList()
  } finally {
    reminderHandleLoading.value = false
  }
}

onMounted(() => {
  handleDeclarationQuery()
  getSupplementList()
  getSettlementList()
  loadWarnings()
})
</script>

<style scoped>
.link-tip {
  margin-top: 8px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
