<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible" width="500px">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="100px"
      v-loading="formLoading"
    >
      <el-form-item label="审核决定" prop="auditDecision">
        <el-radio-group v-model="formData.auditDecision">
          <el-radio :label="true">通过</el-radio>
          <el-radio :label="false">拒绝</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="审核备注" prop="auditRemarks">
        <el-input
          type="textarea"
          v-model="formData.auditRemarks"
          placeholder="请输入审核备注"
          :rows="3"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="submitForm" type="primary" :disabled="formLoading">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { QualificationApi, EnterpriseQualificationAuditReqVO } from '@/api/enterprise/qualification'

defineOptions({ name: 'EnterpriseQualificationAuditForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('审核资质')
const formLoading = ref(false)

const formData = ref<EnterpriseQualificationAuditReqVO>({
  id: undefined!,
  auditDecision: true,
  auditRemarks: ''
})

const formRules = reactive({
  auditDecision: [{ required: true, message: '审核决定不能为空', trigger: 'change' }],
  auditRemarks: [
    { validator: validateAuditRemarks, trigger: 'blur' }
  ]
})

const formRef = ref()

// 自定义校验规则：如果审核拒绝，则审核备注为必填
function validateAuditRemarks(rule: any, value: any, callback: any) {
  if (formData.value.auditDecision === false && !value) {
    callback(new Error('审核拒绝时，审核备注不能为空'))
  } else if (value && value.length > 200) {
    callback(new Error('审核备注长度不能超过 200 个字符'))
  } else {
    callback()
  }
}

/** 打开弹窗 */
const open = async (id: number) => {
  dialogVisible.value = true
  resetForm()
  formData.value.id = id
}

defineExpose({ open })

/** 提交表单 */
const emit = defineEmits(['success'])
const submitForm = async () => {
  if (!formRef.value) return
  await formRef.value.validate()
  formLoading.value = true
  try {
    await QualificationApi.auditQualification(formData.value)
    message.success('审核操作成功')
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}

/** 重置表单 */
const resetForm = () => {
  formData.value = {
    id: undefined!,
    auditDecision: true,
    auditRemarks: ''
  }
  formRef.value?.resetFields()
}
</script> 