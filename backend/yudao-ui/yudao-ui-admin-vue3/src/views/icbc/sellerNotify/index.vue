<template>
  <ContentWrap>
    <el-alert
      type="info"
      :closable="false"
      class="mb-10px"
      title="一期触达只有两条路：短信（默认关闭，费用与到达率是运营成本）与收货员一键把确认链接转达给出售者。首次交易、从未留手机号的场景只有转达这一条通路。"
    />
    <el-alert
      type="warning"
      :closable="false"
      class="mb-10px"
      title="短信只发三条：结算单待确认 / 付款异常 / 发票已开出。内容只说可核验的事，不使用「已到账」的说法。"
    />
    <el-descriptions :column="3" border class="mb-10px">
      <el-descriptions-item label="平台级开关">
        <el-tag :type="setting.platformEnabled ? 'success' : 'info'">
          {{ setting.platformEnabled ? '已开启' : '关闭' }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="本租户开关">
        <el-tag :type="setting.tenantEnabled ? 'success' : 'info'">
          {{ setting.tenantEnabled ? '已开启' : '关闭' }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="自然人端入口">
        <el-tag :type="setting.sellerAppUrlConfigured ? 'success' : 'danger'">
          {{ setting.sellerAppUrlConfigured ? '已配置' : '未配置（拼不出链接）' }}
        </el-tag>
      </el-descriptions-item>
    </el-descriptions>
    <el-form class="-mb-15px" :model="settingForm" :inline="true" label-width="120px">
      <el-form-item label="本租户短信">
        <el-switch v-model="settingForm.smsEnabled" :disabled="!canManage" />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="settingForm.remark" placeholder="谁开的、为什么开" clearable class="!w-280px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="settingSaving" @click="saveSetting"
          v-hasPermi="['icbc:seller-notify:manage']">
          保存开关
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="100px">
      <el-form-item label="触达类型" prop="bizType">
        <el-select v-model="queryParams.bizType" placeholder="请选择" clearable class="!w-200px">
          <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="发送状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择" clearable class="!w-200px">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="出售者" prop="sellerName">
        <el-input v-model="queryParams.sellerName" placeholder="请输入" clearable @keyup.enter="handleQuery" class="!w-180px" />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true">
      <el-table-column label="类型" prop="bizTypeName" min-width="130" />
      <el-table-column label="出售者" prop="sellerName" min-width="100" />
      <el-table-column label="手机号" prop="mobileMasked" width="130" />
      <el-table-column label="状态" align="center" width="160">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)">{{ row.statusName }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="说明 / 失败原因" prop="errorMsg" min-width="200" show-overflow-tooltip />
      <el-table-column label="短信内容" prop="content" min-width="260" show-overflow-tooltip />
      <el-table-column label="发送时间" prop="sendTime" width="170" :formatter="dateFormatter" />
      <el-table-column label="创建时间" prop="createTime" width="170" :formatter="dateFormatter" />
      <el-table-column label="操作" align="center" width="140" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.link" link type="primary" @click="copyText(row.link, '链接已复制')">复制链接</el-button>
          <el-button v-else link disabled>无链接</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </ContentWrap>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useClipboard } from '@vueuse/core'
import { hasPermission } from '@/directives/permission/hasPermi'
import {
  SellerNotifyApi,
  SellerNotifyVO,
  SellerNotifySettingVO,
  SELLER_NOTIFY_TYPES,
  SELLER_NOTIFY_STATUS
} from '@/api/icbc/sellerNotify'
import { dateFormatter } from '@/utils/formatTime'

defineOptions({ name: 'IcbcSellerNotify' })

const message = useMessage()
const { copy } = useClipboard()

const loading = ref(true)
const list = ref<SellerNotifyVO[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  bizType: undefined,
  status: undefined,
  sellerName: undefined
})
const queryFormRef = ref()

const typeOptions = SELLER_NOTIFY_TYPES
const statusOptions = SELLER_NOTIFY_STATUS

const setting = reactive<SellerNotifySettingVO>({})
const settingForm = reactive({ smsEnabled: false, remark: '' })
const settingSaving = ref(false)
const canManage = computed(() => hasPermission(['icbc:seller-notify:manage']))

const getList = async () => {
  loading.value = true
  try {
    const data = await SellerNotifyApi.getNotifyPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const getSetting = async () => {
  const data = await SellerNotifyApi.getSetting()
  Object.assign(setting, data)
  settingForm.smsEnabled = !!data.tenantEnabled
  settingForm.remark = data.remark || ''
}

const saveSetting = async () => {
  settingSaving.value = true
  try {
    await SellerNotifyApi.saveSetting({
      smsEnabled: settingForm.smsEnabled,
      remark: settingForm.remark
    })
    message.success('已保存')
    await getSetting()
  } finally {
    settingSaving.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

const resetQuery = () => {
  queryFormRef.value?.resetFields()
  handleQuery()
}

const statusTag = (status?: number) => {
  if (status === 3) return 'success'
  if (status === 4) return 'danger'
  return 'info'
}

const copyText = async (text: string, tip: string) => {
  await copy(text)
  message.success(tip)
}

onMounted(async () => {
  await Promise.all([getList(), getSetting()])
})
</script>
