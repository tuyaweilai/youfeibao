<template>
  <!-- 回收企业：电子签章开通（#92）。章是租户级的，回收企业才是发起方，平台不代盖。 -->
  <ContentWrap title="电子签章">
    <el-alert
      type="info"
      :closable="false"
      class="mb-10px"
      title="电子签章用于框架收购协议与反向发票合规告知函：两份文书装进一个合同组，一次实名、一次签名签完。企业印章由本企业在第三方控制台完成企业认证后自行创建，平台不代盖。未开通不影响建档——协议自动退回纸质签法。"
    />

    <el-descriptions v-loading="loading" :column="3" border>
      <el-descriptions-item label="子客编号">
        <span>{{ status.subCustomerNo || '-' }}</span>
        <el-tooltip content="由平台生成、持久化、不可变、不可重复；第三方回执靠它找到本企业" placement="top">
          <Icon icon="ep:question-filled" class="ml-5px text-gray-400" />
        </el-tooltip>
      </el-descriptions-item>
      <el-descriptions-item label="开通状态">
        <el-tag :type="statusTagType">{{ status.activationStatusName || '-' }}</el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="企业印章">
        <el-tag v-if="status.sealReady" type="success">印章就位</el-tag>
        <el-tag v-else type="info">印章未就位</el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="合同额度（份）">{{ status.contractQuota ?? 0 }}</el-descriptions-item>
      <el-descriptions-item label="已用（份）">{{ status.contractUsed ?? 0 }}</el-descriptions-item>
      <el-descriptions-item label="剩余（份）">{{ status.remainingQuota ?? 0 }}</el-descriptions-item>
      <el-descriptions-item label="经办人编号">{{ status.operatorNo || '-' }}</el-descriptions-item>
      <el-descriptions-item label="企业印章编号">{{ status.sealNo || '-' }}</el-descriptions-item>
      <el-descriptions-item label="激活时间">
        {{ status.activatedTime ? formatDate(status.activatedTime) : '-' }}
      </el-descriptions-item>
    </el-descriptions>

    <el-alert
      v-if="status.nextStep"
      :type="status.activationStatus === 2 ? 'success' : 'warning'"
      :closable="false"
      class="mt-10px"
      :title="status.nextStep"
    />
    <el-alert
      v-if="status.platformConfigured === false"
      type="error"
      :closable="false"
      class="mt-10px"
      title="平台尚未配置电子签章参数，暂时拿不到开通链接。请联系平台运营补齐环境、应用标识与密钥。"
    />

    <div class="mt-15px">
      <el-button
        type="primary"
        v-hasPermi="['icbc:esign:manage']"
        :loading="opening"
        @click="handleOpen"
      >
        开通电子签
      </el-button>
      <el-button v-hasPermi="['icbc:esign:manage']" @click="openActivateDialog">
        确认已激活
      </el-button>
      <el-button @click="getStatus">刷新</el-button>
    </div>
  </ContentWrap>

  <ContentWrap v-if="consoleLink">
    <el-alert
      type="success"
      :closable="false"
      class="mb-10px"
      title="这是本次的一次性控制台链接，请当场完成企业认证并创建企业印章；再次点「开通电子签」会换一枚新链接，旧链接作废。"
    />
    <el-form label-width="110px">
      <el-form-item label="控制台链接">
        <el-input v-model="consoleLink" readonly>
          <template #append>
            <el-button @click="copyLink">复制</el-button>
          </template>
        </el-input>
      </el-form-item>
      <el-form-item label="有效期止">
        <span>{{ expiresTime ? formatDate(expiresTime) : '-' }}</span>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <Dialog v-model="activateVisible" title="确认电子签章已激活" width="520">
    <el-form ref="activateFormRef" :model="activateForm" label-width="110px">
      <el-form-item
        label="企业印章编号"
        prop="sealNo"
        :rules="[{ required: true, message: '请填写企业印章编号' }]"
      >
        <el-input v-model="activateForm.sealNo" placeholder="在第三方控制台创建企业印章后得到的编号" />
      </el-form-item>
      <el-form-item label="经办人编号">
        <el-input v-model="activateForm.operatorNo" placeholder="选填" />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="activateForm.remark" type="textarea" :rows="2" placeholder="选填" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button type="primary" :loading="activating" @click="submitActivate">确 定</el-button>
      <el-button @click="activateVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import { EsignApi, EsignTenantStatusVO } from '@/api/icbc/esign'
import { formatDate } from '@/utils/formatTime'
import { useClipboard } from '@vueuse/core'

defineOptions({ name: 'IcbcEsign' })

const message = useMessage()
const { copy } = useClipboard()

const loading = ref(false)
const opening = ref(false)
const activating = ref(false)
const status = ref<EsignTenantStatusVO>({})
const consoleLink = ref('')
const expiresTime = ref<Date>()

const activateVisible = ref(false)
const activateFormRef = ref()
const activateForm = reactive<{ operatorNo?: string; sealNo: string; remark?: string }>({
  operatorNo: undefined,
  sealNo: '',
  remark: undefined
})

const statusTagType = computed(() => {
  switch (status.value.activationStatus) {
    case 2:
      return 'success'
    case 1:
      return 'warning'
    default:
      return 'info'
  }
})

const getStatus = async () => {
  loading.value = true
  try {
    status.value = await EsignApi.getStatus()
  } finally {
    loading.value = false
  }
}

const handleOpen = async () => {
  opening.value = true
  try {
    const data = await EsignApi.openConsole()
    consoleLink.value = data.link
    expiresTime.value = data.expiresTime
    message.success('已生成一次性控制台链接')
    await getStatus()
  } finally {
    opening.value = false
  }
}

const copyLink = async () => {
  await copy(consoleLink.value)
  message.success('已复制')
}

const openActivateDialog = () => {
  activateForm.sealNo = status.value.sealNo || ''
  activateForm.operatorNo = status.value.operatorNo
  activateForm.remark = undefined
  activateVisible.value = true
}

const submitActivate = async () => {
  await activateFormRef.value.validate()
  activating.value = true
  try {
    await EsignApi.activate(activateForm)
    message.success('已激活，印章就位')
    activateVisible.value = false
    await getStatus()
  } finally {
    activating.value = false
  }
}

onMounted(getStatus)
</script>
