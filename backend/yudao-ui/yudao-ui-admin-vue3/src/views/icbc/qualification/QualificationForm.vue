<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible">
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="120px" v-loading="formLoading">
      <el-form-item label="资质层" prop="type">
        <el-select v-model="formData.type" class="w-full">
          <el-option label="税务侧（反向开票资格）" value="TAX" />
          <el-option label="行业侧（危废 / 拆解 / 回收备案）" value="INDUSTRY" />
          <el-option label="公安侧（生产性废旧金属备案）" value="PUBLIC_SECURITY" />
        </el-select>
      </el-form-item>
      <el-form-item label="资质名称" prop="name">
        <el-input v-model="formData.name" placeholder="请输入资质名称" />
      </el-form-item>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="发证机关" prop="issuingAuthority">
            <el-input v-model="formData.issuingAuthority" placeholder="选填" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="证书编号" prop="certNo">
            <el-input v-model="formData.certNo" placeholder="选填" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="有效期起" prop="validFrom">
            <el-date-picker v-model="formData.validFrom" type="date" value-format="YYYY-MM-DD" class="w-full" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="有效期止" prop="validTo">
            <el-date-picker v-model="formData.validTo" type="date" value-format="YYYY-MM-DD" class="w-full" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="证照扫描件" prop="fileUrl">
        <el-input v-model="formData.fileUrl" placeholder="选填，文件地址" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="formData.status" class="w-full">
          <el-option label="待核实" :value="0" />
          <el-option label="有效" :value="1" />
          <el-option label="失效" :value="2" />
          <el-option label="吊销" :value="3" />
        </el-select>
      </el-form-item>
      <el-form-item label="核实意见" prop="auditRemark">
        <el-input v-model="formData.auditRemark" type="textarea" :rows="2" placeholder="选填" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="submitForm" type="primary" :disabled="formLoading">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import { QualificationApi, QualificationVO } from '@/api/icbc/qualification'

defineOptions({ name: 'IcbcQualificationForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formData = ref<QualificationVO>(buildEmpty())
const formRef = ref()

function buildEmpty(): QualificationVO {
  return {
    id: undefined,
    type: 'TAX',
    name: undefined,
    issuingAuthority: undefined,
    certNo: undefined,
    validFrom: undefined,
    validTo: undefined,
    fileUrl: undefined,
    status: 0,
    auditRemark: undefined
  }
}

const formRules = reactive({
  type: [{ required: true, message: '资质层不能为空', trigger: 'change' }],
  name: [{ required: true, message: '资质名称不能为空', trigger: 'blur' }],
  status: [{ required: true, message: '状态不能为空', trigger: 'change' }]
})

const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await QualificationApi.getQualification(id)
    } finally {
      formLoading.value = false
    }
  }
}
defineExpose({ open })

const emit = defineEmits(['success'])
const submitForm = async () => {
  await formRef.value.validate()
  formLoading.value = true
  try {
    if (formType.value === 'create') {
      await QualificationApi.createQualification(formData.value)
      message.success(t('common.createSuccess'))
    } else {
      await QualificationApi.updateQualification(formData.value)
      message.success(t('common.updateSuccess'))
    }
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}

const resetForm = () => {
  formData.value = buildEmpty()
  formRef.value?.resetFields()
}
</script>
