<template>
  <!-- 平台运营：电子签章平台级参数 + 各租户开通总览（#92）。密钥只落后端、界面不回显明文。 -->
  <ContentWrap title="电子签章平台参数">
    <el-alert
      type="info"
      :closable="false"
      class="mb-10px"
      title="接入第三方电子签章是配置动作而不是改代码：环境、两套 endpoint、应用标识、密钥、回调地址与验签、签署链接渠道、平台模板都在这里维护。密钥只落后端，界面不回显明文；留空表示不改动既有值。"
    />
    <el-alert
      v-if="config.configured === false"
      type="warning"
      :closable="false"
      class="mb-10px"
      :title="`平台参数尚未齐备，缺少：${(config.missingFields || []).join('、') || '（无）'}`"
    />

    <el-form ref="configFormRef" :model="config" label-width="140px">
      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="环境" prop="environment">
            <el-select v-model="config.environment" clearable class="w-full" placeholder="请选择">
              <el-option label="测试（TEST）" value="TEST" />
              <el-option label="生产（PROD）" value="PROD" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="服务端接口地址">
            <el-input v-model="config.apiEndpoint" placeholder="如 https://ess.tencentcloudapi.com" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="控制台地址">
            <el-input v-model="config.consoleEndpoint" placeholder="如 https://ess.tencent.cn" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="应用标识">
            <el-input v-model="config.appId" placeholder="第三方分配的 AppId" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="应用密钥 ID">
            <el-input
              v-model="config.secretId"
              :placeholder="config.secretIdConfigured ? '已配置，留空不改动' : '请输入'"
            />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="应用密钥">
            <el-input
              v-model="config.secretKey"
              type="password"
              show-password
              :placeholder="config.secretKeyConfigured ? '已配置，留空不改动' : '请输入'"
            />
          </el-form-item>
        </el-col>
        <el-col :span="16">
          <el-form-item label="回调地址">
            <el-input
              v-model="config.callbackUrl"
              placeholder="平台外网可达，如 https://…/admin-api/icbc/esign/callback/notify"
            />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="回调验签密钥">
            <el-input
              v-model="config.callbackSignKey"
              type="password"
              show-password
              :placeholder="config.callbackSignKeyConfigured ? '已配置，留空不改动' : '请输入'"
            />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="签署链接渠道">
            <el-select v-model="config.signLinkChannel" clearable class="w-full" placeholder="请选择">
              <el-option label="H5" value="H5" />
              <el-option label="小程序" value="MINI_PROGRAM" />
              <el-option label="PC" value="PC" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="框架收购协议模板">
            <el-input v-model="config.agreementTemplateId" placeholder="平台模板编号" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="合规告知函模板">
            <el-input v-model="config.noticeTemplateId" placeholder="平台模板编号" />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="备注">
            <el-input v-model="config.remark" type="textarea" :rows="2" placeholder="选填" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item>
        <el-button
          type="primary"
          :loading="saving"
          v-hasPermi="['icbc:platform:esign:manage']"
          @click="saveConfig"
        >
          保存
        </el-button>
        <el-button @click="getConfig">重置</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap title="各租户开通状态与合同额度">
    <el-table v-loading="loading" :data="tenants" :stripe="true">
      <el-table-column label="租户编号" align="center" prop="tenantId" width="100" />
      <el-table-column label="回收企业" align="left" prop="tenantName" min-width="160" />
      <el-table-column label="子客编号" align="center" prop="subCustomerNo" width="160" />
      <el-table-column label="开通状态" align="center" width="110">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.activationStatus)">
            {{ row.activationStatusName || '-' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="企业印章" align="center" width="110">
        <template #default="{ row }">
          <el-tag v-if="row.sealReady" type="success">就位</el-tag>
          <el-tag v-else type="info">未就位</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="额度（份）" align="center" prop="contractQuota" width="100" />
      <el-table-column label="已用（份）" align="center" prop="contractUsed" width="100" />
      <el-table-column label="剩余（份）" align="center" width="100">
        <template #default="{ row }">
          <span :class="{ 'text-red-500 font-bold': row.remainingQuota <= 0 }">
            {{ row.remainingQuota }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="激活时间" align="center" width="170">
        <template #default="{ row }">
          {{ row.activatedTime ? formatDate(row.activatedTime) : '-' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="120" fixed="right">
        <template #default="{ row }">
          <el-button
            link
            type="primary"
            v-hasPermi="['icbc:platform:esign:manage']"
            @click="openQuotaDialog(row)"
          >
            调整额度
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-button class="mt-10px" @click="getTenants">刷新</el-button>
  </ContentWrap>

  <Dialog v-model="quotaVisible" title="调整合同额度" width="460">
    <el-form ref="quotaFormRef" :model="quotaForm" label-width="110px">
      <el-form-item label="租户">
        <span>{{ quotaForm.tenantName }}（{{ quotaForm.tenantId }}）</span>
      </el-form-item>
      <el-form-item
        label="合同额度（份）"
        prop="contractQuota"
        :rules="[{ required: true, message: '请填写合同额度' }]"
      >
        <el-input-number v-model="quotaForm.contractQuota" :min="0" class="w-full" />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="quotaForm.remark" type="textarea" :rows="2" placeholder="选填" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button type="primary" :loading="quotaSaving" @click="submitQuota">确 定</el-button>
      <el-button @click="quotaVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import { EsignConfigVO, PlatformEsignApi, PlatformEsignTenantVO } from '@/api/icbc/esign'
import { formatDate } from '@/utils/formatTime'

defineOptions({ name: 'IcbcPlatformEsign' })

const message = useMessage()

const loading = ref(false)
const saving = ref(false)
const tenants = ref<PlatformEsignTenantVO[]>([])
const configFormRef = ref()
const config = ref<EsignConfigVO>({})

const quotaVisible = ref(false)
const quotaSaving = ref(false)
const quotaFormRef = ref()
const quotaForm = reactive<{
  tenantId?: number
  tenantName?: string
  contractQuota: number
  remark?: string
}>({ tenantId: undefined, tenantName: '', contractQuota: 0, remark: undefined })

const statusTagType = (status?: number) => {
  switch (status) {
    case 2:
      return 'success'
    case 1:
      return 'warning'
    default:
      return 'info'
  }
}

const getConfig = async () => {
  // 密钥字段清空，避免把「已配置」的占位误当明文再次提交
  const data = await PlatformEsignApi.getConfig()
  config.value = { ...data, secretId: '', secretKey: '', callbackSignKey: '' }
}

const saveConfig = async () => {
  saving.value = true
  try {
    await PlatformEsignApi.saveConfig(config.value)
    message.success('已保存')
    await getConfig()
  } finally {
    saving.value = false
  }
}

const getTenants = async () => {
  loading.value = true
  try {
    tenants.value = await PlatformEsignApi.listTenants()
  } finally {
    loading.value = false
  }
}

const openQuotaDialog = (row: PlatformEsignTenantVO) => {
  quotaForm.tenantId = row.tenantId
  quotaForm.tenantName = row.tenantName
  quotaForm.contractQuota = row.contractQuota ?? 0
  quotaForm.remark = undefined
  quotaVisible.value = true
}

const submitQuota = async () => {
  await quotaFormRef.value.validate()
  quotaSaving.value = true
  try {
    await PlatformEsignApi.updateQuota({
      tenantId: quotaForm.tenantId!,
      contractQuota: quotaForm.contractQuota,
      remark: quotaForm.remark
    })
    message.success('已调整')
    quotaVisible.value = false
    await getTenants()
  } finally {
    quotaSaving.value = false
  }
}

onMounted(() => {
  getConfig()
  getTenants()
})
</script>
