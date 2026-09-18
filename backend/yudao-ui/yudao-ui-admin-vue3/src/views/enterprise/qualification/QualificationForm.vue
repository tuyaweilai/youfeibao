<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible" width="600px">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="120px"
      v-loading="formLoading"
    >
      <el-form-item label="企业名称" prop="enterpriseId">
        <el-select v-model="formData.enterpriseId" placeholder="请选择企业" class="w-1/1" filterable>
          <el-option
            v-for="item in enterpriseList"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="资质类型" prop="qualificationType">
        <el-select v-model="formData.qualificationType" placeholder="请选择资质类型" class="w-1/1">
          <el-option
            v-for="dict in getIntDictOptions(DICT_TYPE.ENTERPRISE_QUALIFICATION_TYPE)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="资质名称" prop="qualificationName">
        <el-input v-model="formData.qualificationName" placeholder="请输入资质名称" />
      </el-form-item>
      <el-form-item label="资质编号" prop="qualificationCode">
        <el-input v-model="formData.qualificationCode" placeholder="请输入资质编号" />
      </el-form-item>
      <el-form-item label="发证日期" prop="issueDate">
        <el-date-picker
          v-model="formData.issueDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="选择发证日期"
          class="w-1/1"
        />
      </el-form-item>
      <el-form-item label="到期日期" prop="expiryDate">
        <el-date-picker
          v-model="formData.expiryDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="选择到期日期"
          class="w-1/1"
        />
      </el-form-item>
      <el-form-item label="发证机关" prop="issuingAuthority">
        <el-input v-model="formData.issuingAuthority" placeholder="请输入发证机关" />
      </el-form-item>
      <el-form-item label="资质文件" prop="qualificationFileId">
        <!-- 将单一值转换为数组形式传递给上传组件 -->
        <UploadFile v-model="fileIdArray" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="submitForm" type="primary" :disabled="formLoading">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { QualificationApi, EnterpriseQualificationBaseVO } from '@/api/enterprise/qualification'
// import { InfoApi } from '@/api/enterprise/info' // 用于获取企业列表
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import UploadFile from '@/components/UploadFile/src/UploadFile.vue' // 假设的上传组件路径

defineOptions({ name: 'EnterpriseQualificationForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('') // 'create' | 'update'

const formData = ref<EnterpriseQualificationBaseVO>({
  enterpriseId: undefined!,
  qualificationType: undefined!,
  qualificationName: '',
  qualificationCode: '',
  issueDate: '',
  expiryDate: '',
  issuingAuthority: '',
  qualificationFileId: undefined!
})

const formRules = reactive({
  enterpriseId: [{ required: true, message: '企业名称不能为空', trigger: 'change' }],
  qualificationType: [{ required: true, message: '资质类型不能为空', trigger: 'change' }],
  qualificationName: [
    { required: true, message: '资质名称不能为空', trigger: 'blur' },
    { min: 2, max: 100, message: '长度在 2 到 100 个字符', trigger: 'blur' }
  ],
  qualificationCode: [{ min: 0, max: 50, message: '长度在 0 到 50 个字符', trigger: 'blur' }],
  issuingAuthority: [{ min: 0, max: 100, message: '长度在 0 到 100 个字符', trigger: 'blur' }],
  qualificationFileId: [{ required: true, message: '资质文件不能为空', trigger: 'change' }],
  expiryDate: [
    { validator: validateExpiryDate, trigger: 'blur' }
  ]
})

const formRef = ref()
const enterpriseList = ref<{id: number, name: string}[]>([])

/** 获取企业列表 */
const fetchEnterpriseList = async () => {
  try {
    // 替换为实际的企业列表获取API
    // const data = await InfoApi.getEnterpriseSimpleList() 
    const data = await QualificationApi.getEnterpriseSimpleList() // 使用API文件中mock的
    enterpriseList.value = data
  } catch (error) {
    console.error('获取企业列表失败', error)
    message.error('获取企业列表失败')
  }
}

/** 打开弹窗 */
const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  await fetchEnterpriseList() // 获取企业列表

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

/** 提交表单 */
const emit = defineEmits(['success'])
const submitForm = async () => {
  if (!formRef.value) return
  await formRef.value.validate()
  formLoading.value = true
  try {
    const data = { ...formData.value } as EnterpriseQualificationBaseVO
    // 确保文件ID为数字类型
    if (typeof data.qualificationFileId === 'string') {
      data.qualificationFileId = parseInt(data.qualificationFileId) || 0
    }
    
    if (formType.value === 'create') {
      await QualificationApi.createQualification(data)
      message.success(t('common.createSuccess'))
    } else {
      await QualificationApi.updateQualification(data)
      message.success(t('common.updateSuccess'))
    }
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}

/** 重置表单 */
const resetForm = () => {
  formData.value = {
    enterpriseId: undefined!,
    qualificationType: undefined!,
    qualificationName: '',
    qualificationCode: '',
    issueDate: '',
    expiryDate: '',
    issuingAuthority: '',
    qualificationFileId: undefined!
  }
  formRef.value?.resetFields()
}

// 自定义校验规则：到期日期必须大于发证日期
function validateExpiryDate(rule: any, value: any, callback: any) {
  const issueDate = formData.value.issueDate
  if (issueDate && value && new Date(value) <= new Date(issueDate)) {
    callback(new Error('到期日期必须晚于发证日期'))
  } else {
    callback()
  }
}

// 创建一个计算属性来处理文件ID转换
const fileIdArray = computed({
  get: () => {
    if (!formData.value.qualificationFileId) return []
    return [String(formData.value.qualificationFileId)]
  },
  set: (val: string[]) => {
    formData.value.qualificationFileId = val && val.length > 0 ? parseInt(val[0]) : 0
  }
})

</script> 