<template>
  <!-- 平台运营：卡证识别平台级参数 + 连通性自检（#103）。密钥只落后端、界面不回显明文。
       与电子签章各立一处（ADR 0037），不合并成通用出站依赖配置页。 -->
  <ContentWrap title="卡证识别平台参数">
    <el-alert
      type="info"
      :closable="false"
      class="mb-10px"
      title="接入腾讯云 OCR 是配置动作而不是改代码：供应商（保存后无需重启即生效）/ 密钥 / 地域 / 服务域名 / 超时都在这里维护。密钥只落后端，界面不回显明文；留空表示不改动既有值。"
    />

    <!-- 本票存在的理由：一眼回答「现在到底开了没」。
         四态是显式的（加载中 / 读取失败 / 未启用 / 已启用），不靠 config={} 的字段缺省猜；
         「已配置，识别已启用」只在确证（provider=tencent 且 configured===true）时出现。 -->
    <el-alert
      v-if="status === 'loading'"
      type="info"
      :closable="false"
      class="mb-10px"
      title="正在读取配置…"
      description="读取完成前不判断「已启用 / 未启用」。"
      show-icon
    />
    <el-alert
      v-else-if="status === 'error'"
      type="error"
      :closable="false"
      class="mb-10px"
      title="配置读取失败，未读到任何配置——别当成已启用"
      :description="`${loadError || '请求失败'}。请检查网络或权限后重试。`"
      show-icon
    />
    <el-alert
      v-else-if="status === 'stub'"
      type="warning"
      :closable="false"
      class="mb-10px"
      title="未启用，现场为手工录入"
      description="卡证识别一律返回空结果，建档向导退化为手工输入姓名 / 证件号 / 银行卡号；这不阻断建档。要启用请把供应商改成「腾讯云 OCR」并填齐密钥。"
      show-icon
    />
    <el-alert
      v-else-if="status === 'incomplete'"
      type="warning"
      :closable="false"
      class="mb-10px"
      title="未配齐，现场为手工录入"
      :description="`缺少：${(config.missingFields || []).join('、') || '（无）'}。补齐并保存后即生效。`"
      show-icon
    />
    <el-alert
      v-else-if="status === 'enabled'"
      type="success"
      :closable="false"
      class="mb-10px"
      title="已配置，识别已启用：现场拍照将走腾讯云 OCR"
      description="配置齐备，识别能力已开启。是否真的能连通腾讯云，要看下方「最近自检」的结果——从未自检时不等于验证通过。"
      show-icon
    />
    <el-alert
      v-else
      type="info"
      :closable="false"
      class="mb-10px"
      title="状态未知，别当成已启用"
      description="没拿到明确的「已启用 / 未启用」结论（供应商或配置状态缺失）。请重试读取或核对供应商设置。"
      show-icon
    />
    <el-button v-if="status === 'error'" class="mb-10px" :loading="loading" @click="getConfig">
      重试读取
    </el-button>

    <el-form ref="configFormRef" :model="config" label-width="140px">
      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="供应商" prop="provider">
            <el-select v-model="config.provider" class="w-full" placeholder="请选择">
              <el-option label="未启用（现场手工录入）" value="stub" />
              <el-option label="腾讯云 OCR" value="tencent" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="地域">
            <el-input v-model="config.region" placeholder="如 ap-guangzhou" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="服务域名">
            <el-input v-model="config.endpoint" placeholder="如 ocr.tencentcloudapi.com" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="超时（毫秒）">
            <el-input-number v-model="config.timeout" :min="1" :step="1000" class="w-full" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="SecretId">
            <el-input
              v-model="config.secretId"
              type="password"
              show-password
              :placeholder="config.secretIdConfigured ? '已配置，留空不改动' : '请输入'"
            />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="SecretKey">
            <el-input
              v-model="config.secretKey"
              type="password"
              show-password
              :placeholder="config.secretKeyConfigured ? '已配置，留空不改动' : '请输入'"
            />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="备注">
            <el-input v-model="config.remark" type="textarea" :rows="2" placeholder="选填" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-alert
        v-if="(config.configFileFields || []).length"
        type="info"
        :closable="false"
        class="mb-10px"
        :title="`以下生效值来自配置文件（后台尚未覆盖）：${(config.configFileFields || []).join('、')}`"
      />

      <el-form-item>
        <el-button
          type="primary"
          :loading="saving"
          :disabled="loadState !== 'ready'"
          v-hasPermi="['icbc:platform:card-recognition:manage']"
          @click="saveConfig"
        >
          保存
        </el-button>
        <el-button
          :loading="checking"
          :disabled="loadState !== 'ready'"
          v-hasPermi="['icbc:platform:card-recognition:manage']"
          @click="checkConnectivity"
        >
          连通性自检
        </el-button>
        <el-button @click="getConfig">重置</el-button>
      </el-form-item>
    </el-form>

    <el-alert
      v-if="checkResult.resultName && checkResult.persisted === false"
      type="info"
      :closable="false"
      :title="`临时密钥自检：${checkResult.resultName}${checkResult.checkTime ? '（' + formatDate(checkResult.checkTime) + '）' : ''}`"
      description="本次用的是尚未保存的密钥，未记为「已存配置验证通过」；保存后再点一次自检即可记为已存配置验证。"
      show-icon
    />
    <el-alert
      v-else-if="checkResult.resultName"
      :type="checkResult.ok ? 'success' : 'error'"
      :closable="false"
      :title="`最近自检：${checkResult.resultName}${checkResult.checkTime ? '（' + formatDate(checkResult.checkTime) + '）' : ''}`"
      show-icon
    />
    <el-alert
      v-else
      type="info"
      :closable="false"
      title="尚未自检：填好密钥后可先点「连通性自检」验证，再保存。"
    />
  </ContentWrap>
</template>

<script setup lang="ts">
import { CardRecognitionConfigVO, PlatformCardRecognitionApi } from '@/api/icbc/cardRecognition'
import { formatDate } from '@/utils/formatTime'

defineOptions({ name: 'IcbcPlatformCardRecognition' })

const message = useMessage()

const saving = ref(false)
const checking = ref(false)
const loading = ref(false)
const configFormRef = ref()
const config = ref<CardRecognitionConfigVO>({})

// 显式三态：加载中 / 读取失败 / 已就绪。不看 config={} 的字段缺省去猜「现在到底开了没」。
const loadState = ref<'loading' | 'error' | 'ready'>('loading')
const loadError = ref('')

// 「已启用」只在确证（provider=tencent 且 configured===true）时出现；其余一律不冒充成功。
const status = computed<'loading' | 'error' | 'stub' | 'incomplete' | 'enabled' | 'unknown'>(() => {
  if (loadState.value === 'loading') return 'loading'
  if (loadState.value === 'error') return 'error'
  if (config.value.provider === 'stub') return 'stub'
  if (config.value.provider === 'tencent' && config.value.configured === true) return 'enabled'
  if (config.value.provider === 'tencent' && config.value.configured === false) return 'incomplete'
  return 'unknown'
})

// 自检结果：优先用刚刚这一次的返回值，否则回显库里上一次的分类
const lastCheck = ref<{
  ok?: boolean
  resultName?: string
  checkTime?: Date
  persisted?: boolean
}>({})
const checkResult = computed(() => {
  if (lastCheck.value.resultName) {
    return lastCheck.value
  }
  const result = config.value.lastCheckResult
  return {
    // 与后端同一判据：AUTH_FAILED / NETWORK / SERVICE_NOT_OPENED 算失败，
    // 厂商在识别阶段报错说明鉴权已通过（#112 起 SERVICE_NOT_OPENED 是硬失败：车牌识别接口没开通）
    ok: result === 'OK' || result === 'VENDOR_ERROR',
    resultName: config.value.lastCheckResultName,
    checkTime: config.value.lastCheckTime,
    // 库里存的只可能是「用已存配置」的自检（临时凭据不落库），故为 true
    persisted: true
  }
})

const getConfig = async () => {
  loading.value = true
  loadState.value = 'loading'
  loadError.value = ''
  try {
    // 密钥字段清空，避免把「已配置」的占位误当明文再次提交
    const data = await PlatformCardRecognitionApi.getConfig()
    config.value = { ...data, secretId: '', secretKey: '' }
    lastCheck.value = {}
    loadState.value = 'ready'
  } catch (e: any) {
    // axios 只弹一条 toast；若在这里吞掉异常，config 会永远停在 {}，
    // 以前落到 v-else 就会持续断言「已启用」——正是最该说实话的时候说反。
    config.value = {}
    lastCheck.value = {}
    loadError.value = e?.message || '请求失败'
    loadState.value = 'error'
  } finally {
    loading.value = false
  }
}

const saveConfig = async () => {
  saving.value = true
  try {
    await PlatformCardRecognitionApi.saveConfig(config.value)
    message.success('已保存')
    await getConfig()
  } finally {
    saving.value = false
  }
}

const checkConnectivity = async () => {
  checking.value = true
  try {
    // 带上当前填写的密钥（可能尚未保存）：留空则由后端用已存值，先验证、再保存
    const result = await PlatformCardRecognitionApi.check({
      secretId: config.value.secretId,
      secretKey: config.value.secretKey,
      region: config.value.region,
      endpoint: config.value.endpoint,
      timeout: config.value.timeout
    })
    lastCheck.value = result
    if (result.ok) {
      message.success(
        result.persisted === false
          ? `自检通过（临时密钥，未落库）：${result.resultName}`
          : `自检通过：${result.resultName}`
      )
    } else {
      message.error(`自检失败：${result.resultName}`)
    }
  } finally {
    checking.value = false
  }
}

onMounted(() => {
  getConfig()
})
</script>
