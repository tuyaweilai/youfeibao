<template>
  <ContentWrap>
    <el-alert
      type="info"
      :closable="false"
      class="mb-10px"
      title="采购订单是采购执行依据（一个合同 → 多个订单 → 多次收货）：向谁买哪些品类、多少量、什么价、在哪段时间、哪个场站。它不是交易对方下的单（那是「到站预约」）；零散收购可以不挂订单。只有「执行中」的订单可作为采购依据。"
    />
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="90px">
      <el-form-item label="订单号" prop="orderNo">
        <el-input v-model="queryParams.orderNo" placeholder="请输入" clearable @keyup.enter="handleQuery" class="!w-180px" />
      </el-form-item>
      <el-form-item label="交易对方" prop="counterpartyName">
        <el-input v-model="queryParams.counterpartyName" placeholder="请输入" clearable @keyup.enter="handleQuery" class="!w-180px" />
      </el-form-item>
      <el-form-item label="主体类型" prop="counterpartyType">
        <el-select v-model="queryParams.counterpartyType" placeholder="请选择" clearable class="!w-160px">
          <el-option v-for="item in PURCHASE_ORDER_COUNTERPARTY_TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="执行场站" prop="stationId">
        <el-select v-model="queryParams.stationId" placeholder="请选择" clearable class="!w-160px">
          <el-option v-for="item in stationOptions" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择" clearable class="!w-140px">
          <el-option v-for="item in PURCHASE_ORDER_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" plain @click="openForm('create')" v-hasPermi="['icbc:purchase-order:manage']">
          <Icon icon="ep:plus" class="mr-5px" /> 新增订单
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true">
      <el-table-column label="订单号" prop="orderNo" min-width="200" />
      <el-table-column label="交易对方" prop="counterpartyName" min-width="140" />
      <el-table-column label="主体类型" prop="counterpartyTypeName" width="130" />
      <el-table-column label="执行场站" width="140">
        <template #default="{ row }">{{ row.stationName || '-' }}</template>
      </el-table-column>
      <el-table-column label="执行期间" min-width="200">
        <template #default="{ row }">{{ row.startDate }} ~ {{ row.endDate }}</template>
      </el-table-column>
      <el-table-column label="计划量" width="120" align="right">
        <template #default="{ row }">{{ row.totalQuantity ?? '-' }}</template>
      </el-table-column>
      <el-table-column label="计划金额" width="140" align="right">
        <template #default="{ row }">{{ row.totalAmount ?? '-' }}</template>
      </el-table-column>
      <el-table-column label="合同" min-width="180">
        <template #default="{ row }">
          <span v-if="row.contractNo" class="text-primary">{{ row.contractNo }}</span>
          <span v-else class="text-gray-400">-</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" width="110">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row)">{{ row.statusName }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="360" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">明细</el-button>
          <el-button link type="info" @click="openProgress(row)" v-hasPermi="['icbc:purchase-order:query']">
            执行进度
          </el-button>
          <el-button
            v-if="row.status === 0"
            link
            type="primary"
            @click="openForm('update', row.id)"
            v-hasPermi="['icbc:purchase-order:manage']"
          >
            编辑
          </el-button>
          <el-button
            v-if="row.status === 0"
            link
            type="success"
            @click="changeStatus(row, 1)"
            v-hasPermi="['icbc:purchase-order:manage']"
          >
            开始执行
          </el-button>
          <el-button
            v-if="row.status === 1"
            link
            type="warning"
            @click="openReason('suspend', row)"
            v-hasPermi="['icbc:purchase-order:manage']"
          >
            暂停
          </el-button>
          <el-button
            v-if="row.status === 2"
            link
            type="success"
            @click="changeStatus(row, 1)"
            v-hasPermi="['icbc:purchase-order:manage']"
          >
            恢复
          </el-button>
          <el-button
            v-if="row.status === 1"
            link
            type="primary"
            @click="changeStatus(row, 3)"
            v-hasPermi="['icbc:purchase-order:manage']"
          >
            完成
          </el-button>
          <el-button
            v-if="row.status === 1"
            link
            type="info"
            @click="openDeal(row)"
            v-hasPermi="['icbc:purchase-order:manage']"
          >
            记录成交
          </el-button>
          <el-button
            v-if="row.status !== 4"
            link
            type="danger"
            @click="openReason('close', row)"
            v-hasPermi="['icbc:purchase-order:manage']"
          >
            关闭
          </el-button>
          <el-button
            v-if="row.status === 0"
            link
            type="danger"
            @click="handleDelete(row.id)"
            v-hasPermi="['icbc:purchase-order:manage']"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </ContentWrap>

  <!-- 新增 / 编辑 -->
  <el-dialog v-model="formVisible" :title="formTitle" width="960px">
    <el-alert
      type="info"
      :closable="false"
      class="mb-10px"
      title="一单可含多条品类明细，一条明细可分多次收货；只有草稿可以编辑。关联合同时必须是已审核生效且未过期的合同。"
    />
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="130px" v-loading="formLoading">
      <el-form-item label="交易对方类型" prop="counterpartyType">
        <el-select v-model="formData.counterpartyType" class="!w-220px" @change="handleCounterpartyTypeChange">
          <el-option v-for="item in PURCHASE_ORDER_COUNTERPARTY_TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="formData.counterpartyType === PURCHASE_ORDER_NATURAL_COUNTERPARTY_TYPE" label="自然人出售者" prop="payeeId">
        <el-select v-model="formData.payeeId" filterable placeholder="从出售者档案中选择" class="!w-360px">
          <el-option
            v-for="item in payeeOptions"
            :key="item.id"
            :label="item.name + (item.mobile ? '（' + item.mobile + '）' : '')"
            :value="item.id!"
          />
        </el-select>
      </el-form-item>
      <el-form-item v-else label="单位供货方" prop="supplierId">
        <el-select v-model="formData.supplierId" filterable placeholder="从单位供货方中选择" class="!w-360px" @change="handleSupplierChange">
          <el-option v-for="item in supplierOptions" :key="item.id" :label="item.name" :value="item.id!" />
        </el-select>
      </el-form-item>
      <el-form-item label="关联合同" prop="contractId">
        <el-select v-model="formData.contractId" filterable clearable placeholder="可选：只列已生效合同" class="!w-360px">
          <el-option v-for="item in contractOptions" :key="item.id" :label="item.contractNo + ' ' + item.name" :value="item.id!" />
        </el-select>
      </el-form-item>
      <el-form-item label="执行场站" prop="stationId">
        <el-select v-model="formData.stationId" clearable placeholder="可选" class="!w-360px">
          <el-option v-for="item in stationOptions" :key="item.id" :label="item.name" :value="item.id!" />
        </el-select>
      </el-form-item>
      <el-form-item label="执行期间" prop="startDate">
        <el-date-picker v-model="formData.startDate" type="date" value-format="YYYY-MM-DD" placeholder="开始日期" class="!w-200px" />
        <span class="mx-8px">至</span>
        <el-date-picker v-model="formData.endDate" type="date" value-format="YYYY-MM-DD" placeholder="结束日期" class="!w-200px" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" type="textarea" :rows="2" placeholder="选填" />
      </el-form-item>
    </el-form>

    <el-divider content-position="left">品类明细（至少一条）</el-divider>
    <el-button type="primary" plain size="small" class="mb-10px" @click="addItem">
      <Icon icon="ep:plus" class="mr-5px" /> 添加明细
    </el-button>
    <el-table :data="formData.items" size="small">
      <el-table-column label="品类" min-width="180">
        <template #default="{ row }">
          <el-select v-model="row.goodsConfigId" filterable placeholder="选择品类" class="!w-100%">
            <el-option v-for="item in goodsOptions" :key="item.id" :label="item.name + '（' + (item.unit || '-') + '）'" :value="item.id!" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="计划量" width="130">
        <template #default="{ row }">
          <el-input-number v-model="row.quantity" :min="0" :precision="4" :controls="false" class="!w-100%" />
        </template>
      </el-table-column>
      <el-table-column label="定价方式" width="160">
        <template #default="{ row }">
          <el-select v-model="row.priceMode" class="!w-100%">
            <el-option v-for="item in PURCHASE_ORDER_PRICE_MODE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="参考单价" width="130">
        <template #default="{ row }">
          <el-input-number v-model="row.unitPrice" :min="0" :precision="4" :controls="false" class="!w-100%" />
        </template>
      </el-table-column>
      <el-table-column label="价格表" width="110" align="center">
        <template #default="{ row, $index }">
          <el-button v-if="row.priceMode === 2" link type="primary" @click="openPriceTable($index)">
            价格表({{ (row.prices || []).length }})
          </el-button>
          <span v-else class="text-gray-400">-</span>
        </template>
      </el-table-column>
      <el-table-column label="备注" min-width="140">
        <template #default="{ row }">
          <el-input v-model="row.remark" placeholder="等级 / 规格" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="70" align="center">
        <template #default="{ $index }">
          <el-button link type="danger" @click="removeItem($index)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <template #footer>
      <el-button @click="formVisible = false">取 消</el-button>
      <el-button type="primary" :loading="formLoading" @click="submitForm">确 定</el-button>
    </template>
  </el-dialog>

  <!-- 交货日价格表 -->
  <el-dialog v-model="priceVisible" title="交货日价格表" width="640px">
    <el-alert
      type="info"
      :closable="false"
      class="mb-10px"
      title="一条 = 某个交货日生效的单价；成交时取「不晚于交货日的最新一条」，没覆盖到回退参考单价。"
    />
    <el-button type="primary" plain size="small" class="mb-10px" @click="addPrice">添加价格</el-button>
    <el-table :data="priceRows" size="small">
      <el-table-column label="生效交货日" min-width="180">
        <template #default="{ row }">
          <el-date-picker v-model="row.deliveryDate" type="date" value-format="YYYY-MM-DD" class="!w-100%" />
        </template>
      </el-table-column>
      <el-table-column label="单价" width="150">
        <template #default="{ row }">
          <el-input-number v-model="row.unitPrice" :min="0" :precision="4" :controls="false" class="!w-100%" />
        </template>
      </el-table-column>
      <el-table-column label="备注" min-width="140">
        <template #default="{ row }">
          <el-input v-model="row.remark" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="70" align="center">
        <template #default="{ $index }">
          <el-button link type="danger" @click="priceRows.splice($index, 1)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <template #footer>
      <el-button @click="priceVisible = false">取 消</el-button>
      <el-button type="primary" @click="confirmPriceTable">确 定</el-button>
    </template>
  </el-dialog>

  <!-- 记录成交 -->
  <el-dialog v-model="dealVisible" title="记录成交（价格快照）" width="560px">
    <el-alert type="info" :closable="false" class="mb-10px" title="每次成交留价格快照与调整原因；成交价与参考价不一致时必须说明。" />
    <el-form label-width="100px">
      <el-form-item label="订单">
        <span>{{ current?.orderNo }}</span>
      </el-form-item>
      <el-form-item label="品类明细">
        <el-select v-model="dealForm.itemId" placeholder="选择明细" class="!w-100%">
          <el-option
            v-for="item in current?.items || []"
            :key="item.id"
            :label="item.categoryName + '（计划 ' + item.quantity + (item.unit || '') + '）'"
            :value="item.id!"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="交货日">
        <el-date-picker v-model="dealForm.deliveryDate" type="date" value-format="YYYY-MM-DD" class="!w-100%" />
      </el-form-item>
      <el-form-item label="成交数量">
        <el-input-number v-model="dealForm.quantity" :min="0" :precision="4" :controls="false" class="!w-100%" />
      </el-form-item>
      <el-form-item label="成交单价">
        <el-input-number v-model="dealForm.unitPrice" :min="0" :precision="4" :controls="false" class="!w-100%" />
      </el-form-item>
      <el-form-item label="调整原因">
        <el-input v-model="dealForm.adjustReason" type="textarea" :rows="2" placeholder="成交价与参考价不一致时必填" />
      </el-form-item>
      <el-form-item label="来源单号">
        <el-input v-model="dealForm.sourceNo" placeholder="选填：如收购单号" />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="dealForm.remark" type="textarea" :rows="2" placeholder="选填" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dealVisible = false">取 消</el-button>
      <el-button type="primary" @click="submitDeal">确 定</el-button>
    </template>
  </el-dialog>

  <!-- 执行进度 -->
  <el-dialog v-model="progressVisible" title="执行进度" width="720px">
    <template v-if="progress">
      <el-alert type="info" :closable="false" class="mb-10px" :title="progress.scopeNote" />
      <el-descriptions :column="3" border class="mb-10px">
        <el-descriptions-item label="订单号">{{ progress.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ progress.statusName }}</el-descriptions-item>
        <el-descriptions-item label="计划金额">{{ progress.totalAmount ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="计划量">{{ progress.totalQuantity ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="已收量">{{ progress.receivedQuantity ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="未收量">{{ progress.remainingQuantity ?? '-' }}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="progress.items" size="small">
        <el-table-column label="品类" prop="categoryName" min-width="140" />
        <el-table-column label="单位" prop="unit" width="70" />
        <el-table-column label="计划量" prop="quantity" width="110" align="right" />
        <el-table-column label="已收量" prop="receivedQuantity" width="110" align="right" />
        <el-table-column label="未收量" prop="remainingQuantity" width="110" align="right" />
        <el-table-column label="成交笔数" prop="dealCount" width="90" align="center" />
      </el-table>
    </template>
    <template #footer>
      <el-button @click="progressVisible = false">关 闭</el-button>
    </template>
  </el-dialog>

  <!-- 明细 -->
  <el-dialog v-model="detailVisible" title="采购订单明细" width="900px">
    <template v-if="detail">
      <el-descriptions :column="3" border class="mb-10px">
        <el-descriptions-item label="订单号">{{ detail.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTagType(detail)">{{ detail.statusName }}</el-tag>
          <el-tag v-if="detail.usableAsPurchaseBasis" type="success" class="ml-5px">可作为采购依据</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="交易对方">{{ detail.counterpartyName }}</el-descriptions-item>
        <el-descriptions-item label="主体类型">{{ detail.counterpartyTypeName }}</el-descriptions-item>
        <el-descriptions-item label="执行场站">{{ detail.stationName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="关联合同">{{ detail.contractNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="执行期间" :span="3">{{ detail.startDate }} ~ {{ detail.endDate }}</el-descriptions-item>
        <el-descriptions-item label="暂停原因" :span="3">{{ detail.suspendReason || '-' }}</el-descriptions-item>
        <el-descriptions-item label="关闭原因" :span="3">{{ detail.closeReason || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="3">{{ detail.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
      <el-divider content-position="left">品类明细（一条明细可分多次收货）</el-divider>
      <el-table :data="detail.items" size="small" class="mb-10px">
        <el-table-column label="品类" prop="categoryName" min-width="120" />
        <el-table-column label="单位" prop="unit" width="70" />
        <el-table-column label="计划量" prop="quantity" width="100" align="right" />
        <el-table-column label="定价方式" prop="priceModeName" width="130" />
        <el-table-column label="参考单价" prop="unitPrice" width="110" align="right" />
        <el-table-column label="已收量" prop="receivedQuantity" width="100" align="right" />
        <el-table-column label="未收量" prop="remainingQuantity" width="100" align="right" />
        <el-table-column label="成交笔数" prop="dealCount" width="90" align="center" />
      </el-table>
      <el-divider content-position="left">成交记录（价格快照，只追加）</el-divider>
      <el-table :data="deals" size="small">
        <el-table-column label="成交单号" prop="dealNo" min-width="200" />
        <el-table-column label="品类" prop="categoryName" width="110" />
        <el-table-column label="交货日" prop="deliveryDate" width="120" />
        <el-table-column label="数量" prop="quantity" width="100" align="right" />
        <el-table-column label="成交单价" prop="unitPrice" width="110" align="right" />
        <el-table-column label="参考单价" prop="referenceUnitPrice" width="110" align="right" />
        <el-table-column label="调整原因" prop="adjustReason" min-width="140" show-overflow-tooltip />
        <el-table-column label="来源单号" prop="sourceNo" min-width="140" />
        <el-table-column label="成交时间" prop="dealTime" width="170" :formatter="dateFormatter" />
      </el-table>
    </template>
    <template #footer>
      <el-button @click="detailVisible = false">关 闭</el-button>
    </template>
  </el-dialog>

  <!-- 暂停 / 关闭原因 -->
  <el-dialog v-model="reasonVisible" :title="reasonTitle" width="480px">
    <el-alert
      v-if="reasonAction === 'suspend'"
      type="warning"
      :closable="false"
      class="mb-10px"
      title="暂停后订单不再作为采购依据，恢复后继续。"
    />
    <el-alert
      v-else
      type="warning"
      :closable="false"
      class="mb-10px"
      title="关闭后订单不再作为采购依据；已发生的业务不因关闭而消失。"
    />
    <el-input v-model="reasonForm.reason" type="textarea" :rows="3" :placeholder="reasonAction === 'suspend' ? '暂停原因（必填）' : '关闭原因（选填）'" />
    <template #footer>
      <el-button @click="reasonVisible = false">取 消</el-button>
      <el-button type="primary" @click="submitReason">确 定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import {
  PurchaseOrderApi,
  PurchaseOrderVO,
  PurchaseOrderItemVO,
  PurchaseOrderDealVO,
  PURCHASE_ORDER_STATUS_OPTIONS,
  PURCHASE_ORDER_PRICE_MODE_OPTIONS,
  PURCHASE_ORDER_COUNTERPARTY_TYPE_OPTIONS,
  PURCHASE_ORDER_NATURAL_COUNTERPARTY_TYPE
} from '@/api/icbc/purchaseOrder'
import { PurchaseContractApi } from '@/api/icbc/purchaseContract'
import { PayeeApi } from '@/api/icbc/payee'
import { GoodsConfigApi } from '@/api/icbc/goodsConfig'
import { StationApi } from '@/api/icbc/station'
import { SupplierApi } from '@/api/erp/purchase/supplier'
import { dateFormatter } from '@/utils/formatTime'

defineOptions({ name: 'IcbcPurchaseOrder' })

const message = useMessage()

const loading = ref(true)
const list = ref<PurchaseOrderVO[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  orderNo: undefined,
  counterpartyName: undefined,
  counterpartyType: undefined,
  stationId: undefined,
  status: undefined
})
const queryFormRef = ref()

const payeeOptions = ref<any[]>([])
const supplierOptions = ref<any[]>([])
const goodsOptions = ref<any[]>([])
const stationOptions = ref<any[]>([])
const contractOptions = ref<any[]>([])

const getList = async () => {
  loading.value = true
  try {
    const data = await PurchaseOrderApi.getPurchaseOrderPage(queryParams)
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
  queryFormRef.value.resetFields()
  handleQuery()
}

const statusTagType = (row: PurchaseOrderVO) => {
  if (row.status === 1) return row.expired ? 'warning' : 'success'
  if (row.status === 2) return 'warning'
  if (row.status === 3) return 'success'
  return 'info'
}

// 下拉：只在需要时加载一次
const loadOptions = async () => {
  if (!payeeOptions.value.length) {
    const page = await PayeeApi.getPayeePage({ pageNo: 1, pageSize: 100 })
    payeeOptions.value = page.list || []
  }
  if (!supplierOptions.value.length) {
    supplierOptions.value = (await SupplierApi.getSupplierSimpleList()) || []
  }
  if (!goodsOptions.value.length) {
    goodsOptions.value = (await GoodsConfigApi.getEnabledList()) || []
  }
  if (!stationOptions.value.length) {
    const page = await StationApi.getStationPage({ pageNo: 1, pageSize: 100 })
    stationOptions.value = page.list || []
  }
  if (!contractOptions.value.length) {
    // 只有已生效（status=2）且未过期的合同才能作为采购依据；后端也会再校验一次
    const page = await PurchaseContractApi.getPurchaseContractPage({ pageNo: 1, pageSize: 100, status: 2 })
    contractOptions.value = page.list || []
  }
}

// ==================== 新增 / 编辑 ====================
const formVisible = ref(false)
const formLoading = ref(false)
const formMode = ref<'create' | 'update'>('create')
const formTitle = computed(() => (formMode.value === 'create' ? '新增采购订单' : '编辑采购订单'))
const formRef = ref()
const formData = reactive<PurchaseOrderVO>({ items: [] })

const formRules = reactive({
  counterpartyType: [{ required: true, message: '请选择交易对方类型', trigger: 'change' }],
  payeeId: [{ required: true, message: '请选择自然人出售者', trigger: 'change' }],
  supplierId: [{ required: true, message: '请选择单位供货方', trigger: 'change' }],
  startDate: [{ required: true, message: '请选择执行开始日期', trigger: 'change' }],
  endDate: [{ required: true, message: '请选择执行结束日期', trigger: 'change' }]
})

const resetFormData = () => {
  Object.assign(formData, {
    id: undefined,
    contractId: undefined,
    counterpartyType: PURCHASE_ORDER_NATURAL_COUNTERPARTY_TYPE,
    payeeId: undefined,
    supplierId: undefined,
    counterpartyName: undefined,
    stationId: undefined,
    startDate: undefined,
    endDate: undefined,
    remark: undefined,
    items: []
  })
}

const addItem = () => {
  formData.items!.push({
    goodsConfigId: undefined,
    quantity: undefined,
    priceMode: 1,
    unitPrice: undefined,
    prices: [],
    remark: undefined
  })
}
const removeItem = (index: number) => {
  formData.items!.splice(index, 1)
}

const openForm = async (mode: 'create' | 'update', id?: number) => {
  formMode.value = mode
  resetFormData()
  await loadOptions()
  if (mode === 'update' && id) {
    const detail = await PurchaseOrderApi.getPurchaseOrder(id)
    Object.assign(formData, {
      id: detail.id,
      contractId: detail.contractId,
      counterpartyType: detail.counterpartyType,
      payeeId: detail.payeeId,
      supplierId: detail.supplierId,
      counterpartyName: detail.counterpartyName,
      stationId: detail.stationId,
      startDate: detail.startDate,
      endDate: detail.endDate,
      remark: detail.remark,
      items: (detail.items || []).map((item) => ({
        goodsConfigId: item.goodsConfigId,
        quantity: item.quantity,
        priceMode: item.priceMode,
        unitPrice: item.unitPrice,
        prices: (item.prices || []).map((price) => ({ ...price })),
        remark: item.remark
      }))
    })
  }
  formVisible.value = true
}

const handleCounterpartyTypeChange = () => {
  if (formData.counterpartyType === PURCHASE_ORDER_NATURAL_COUNTERPARTY_TYPE) {
    formData.supplierId = undefined
    formData.counterpartyName = undefined
  } else {
    formData.payeeId = undefined
    formData.counterpartyName = undefined
  }
}
const handleSupplierChange = (id: number) => {
  const supplier = supplierOptions.value.find((item) => item.id === id)
  formData.counterpartyName = supplier?.name
}

const submitForm = async () => {
  if (!formData.items || !formData.items.length) {
    message.warning('至少需要一条品类明细')
    return
  }
  await formRef.value.validate()
  formLoading.value = true
  try {
    if (formMode.value === 'create') {
      await PurchaseOrderApi.createPurchaseOrder(formData)
      message.success('已创建草稿，开始执行后才可作为采购依据')
    } else {
      await PurchaseOrderApi.updatePurchaseOrder(formData)
      message.success('已保存')
    }
    formVisible.value = false
    await getList()
  } finally {
    formLoading.value = false
  }
}

// ==================== 交货日价格表 ====================
const priceVisible = ref(false)
const priceRows = ref<any[]>([])
const priceItemIndex = ref(-1)
const openPriceTable = (index: number) => {
  priceItemIndex.value = index
  priceRows.value = ((formData.items![index].prices as any[]) || []).map((price) => ({ ...price }))
  if (!priceRows.value.length) {
    priceRows.value.push({ deliveryDate: undefined, unitPrice: undefined, remark: undefined })
  }
  priceVisible.value = true
}
const addPrice = () => {
  priceRows.value.push({ deliveryDate: undefined, unitPrice: undefined, remark: undefined })
}
const confirmPriceTable = () => {
  formData.items![priceItemIndex.value].prices = priceRows.value.map((price) => ({ ...price }))
  priceVisible.value = false
}

// ==================== 记录成交 ====================
const dealVisible = ref(false)
const current = ref<PurchaseOrderVO>()
const dealForm = reactive<PurchaseOrderDealVO>({})
const openDeal = async (row: PurchaseOrderVO) => {
  current.value = await PurchaseOrderApi.getPurchaseOrder(row.id!)
  Object.assign(dealForm, {
    orderId: row.id,
    itemId: current.value?.items?.[0]?.id,
    deliveryDate: undefined,
    quantity: undefined,
    unitPrice: undefined,
    adjustReason: undefined,
    sourceType: undefined,
    sourceNo: undefined,
    remark: undefined
  })
  dealVisible.value = true
}
const submitDeal = async () => {
  if (!dealForm.itemId || !dealForm.quantity || dealForm.unitPrice === undefined) {
    message.warning('请选择明细并填写数量与成交单价')
    return
  }
  await PurchaseOrderApi.createDeal(dealForm)
  message.success('已记录成交（价格快照）')
  dealVisible.value = false
  await getList()
}

// ==================== 执行进度 ====================
const progressVisible = ref(false)
const progress = ref<any>()
const openProgress = async (row: PurchaseOrderVO) => {
  progress.value = await PurchaseOrderApi.getProgress(row.id!)
  progressVisible.value = true
}

// ==================== 明细 ====================
const detailVisible = ref(false)
const detail = ref<PurchaseOrderVO>()
const deals = ref<PurchaseOrderDealVO[]>([])
const openDetail = async (row: PurchaseOrderVO) => {
  detail.value = await PurchaseOrderApi.getPurchaseOrder(row.id!)
  deals.value = await PurchaseOrderApi.getDealList(row.id!)
  detailVisible.value = true
}

// ==================== 状态流转 ====================
const changeStatus = async (row: PurchaseOrderVO, status: number) => {
  await PurchaseOrderApi.updatePurchaseOrderStatus({ id: row.id!, status })
  message.success('已更新状态')
  await getList()
}

const reasonVisible = ref(false)
const reasonAction = ref<'suspend' | 'close'>('suspend')
const reasonForm = reactive<{ id?: number; status?: number; reason?: string }>({})
const reasonTitle = computed(() => (reasonAction.value === 'suspend' ? '暂停采购订单' : '关闭采购订单'))
const openReason = (action: 'suspend' | 'close', row: PurchaseOrderVO) => {
  reasonAction.value = action
  reasonForm.id = row.id
  reasonForm.status = action === 'suspend' ? 2 : 4
  reasonForm.reason = undefined
  reasonVisible.value = true
}
const submitReason = async () => {
  if (reasonAction.value === 'suspend' && !reasonForm.reason?.trim()) {
    message.warning('暂停必须填写原因')
    return
  }
  await PurchaseOrderApi.updatePurchaseOrderStatus({
    id: reasonForm.id!,
    status: reasonForm.status!,
    reason: reasonForm.reason
  })
  message.success(reasonAction.value === 'suspend' ? '已暂停' : '已关闭')
  reasonVisible.value = false
  await getList()
}

// ==================== 删除 ====================
const handleDelete = async (id?: number) => {
  if (!id) return
  await message.delConfirm()
  await PurchaseOrderApi.deletePurchaseOrder(id)
  message.success('已删除')
  await getList()
}

getList()
</script>
