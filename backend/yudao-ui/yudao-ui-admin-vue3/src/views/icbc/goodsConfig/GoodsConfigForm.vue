<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible">
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="130px" v-loading="formLoading">
      <el-form-item label="品类名称" prop="name">
        <el-input v-model="formData.name" placeholder="如：废铁" />
      </el-form-item>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="计量单位" prop="unit">
            <el-input v-model="formData.unit" placeholder="如：吨" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="税率" prop="taxRate">
            <el-input-number v-model="formData.taxRate" :min="0" :max="1" :precision="2" :step="0.01" class="w-full" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="税收分类合并编码" prop="mergedCode">
        <el-input v-model="formData.mergedCode" placeholder="19 位商品和服务税收分类合并编码" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="formData.status" class="w-full">
          <el-option label="启用" :value="0" />
          <el-option label="停用" :value="1" />
        </el-select>
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" type="textarea" :rows="2" placeholder="选填" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="submitForm" type="primary" :disabled="formLoading">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import { GoodsConfigApi, GoodsConfigVO } from '@/api/icbc/goodsConfig'

defineOptions({ name: 'IcbcGoodsConfigForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formData = ref<GoodsConfigVO>(buildEmpty())
const formRef = ref()

function buildEmpty(): GoodsConfigVO {
  return { id: undefined, name: undefined, unit: '吨', taxRate: 0.01, mergedCode: undefined, status: 0, remark: undefined }
}

const formRules = reactive({
  name: [{ required: true, message: '品类名称不能为空', trigger: 'blur' }],
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
      formData.value = await GoodsConfigApi.getGoodsConfig(id)
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
      await GoodsConfigApi.createGoodsConfig(formData.value)
      message.success(t('common.createSuccess'))
    } else {
      await GoodsConfigApi.updateGoodsConfig(formData.value)
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
