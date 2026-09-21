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

    <!-- 本票存在的理由：一眼回答「现在到底开了没」 -->
    <el-alert
      v-if="config.provider === 'stub'"
      type="warning"
      :closable="false"
      class="mb-10px"
      title="未启用，现场为手工录入"
      description="卡证识别一律返回空结果，建档向导退化为手工输入姓名 / 证件号 / 银行卡号；这不阻断建档。要启用请把供应商改成「腾讯云 OCR」并填齐密钥。"
      show-icon
    />
    <el-alert
      v-else-if="config.configured === false"
      type="warning"
      :closable="false"
      class="mb-10px"
      title="未配齐，现场为手工录入"
      :description="`缺少：${(config.missingFields || []).join('、') || '（无）'}。补齐并保存后即生效。`"
      show-icon
    />
    <el-alert
      v-else
      type="success"
      :closable="false"
      class="mb-10px"
      title="已配置，识别已启用：现场拍照将走腾讯云 OCR"
      show-icon
    />

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
          v-hasPermi="['icbc:platform:card-recognition:manage']"
          @click="saveConfig"
        >
          保存
        </el-button>
        <el-button
          :loading="checking"
          v-hasPermi="['icbc:platform:card-recognition:manage']"
          @click="checkConnectivity"
        >
          连通性自检
        </el-button>
        <el-button @click="getConfig">重置</el-button>
      </el-form-item>
    </el-form>

    <el-alert
      v-if="checkResult.resultName"
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
const configFormRef = ref()
const config = ref<CardRecognitionConfigVO>({})

// 自检结果：优先用刚刚这一次的返回值，否则回显库里上一次的分类
const lastCheck = ref<{ ok?: boolean; resultName?: string; checkTime?: Date }>({})
const checkResult = computed(() => {
  if (lastCheck.value.resultName) {
    return lastCheck.value
  }
  const result = config.value.lastCheckResult
  return {
    // 与后端同一判据：只有 AUTH_FAILED / NETWORK 算失败，厂商在识别阶段报错说明鉴权已通过
    ok: result === 'OK' || result === 'VENDOR_ERROR',
    resultName: config.value.lastCheckResultName,
    checkTime: config.value.lastCheckTime
  }
})

const getConfig = async () => {
  // 密钥字段清空，避免把「已配置」的占位误当明文再次提交
  const data = await PlatformCardRecognitionApi.getConfig()
  config.value = { ...data, secretId: '', secretKey: '' }
  lastCheck.value = {}
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
      message.success(`自检通过：${result.resultName}`)
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
