<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible">
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="150px" v-loading="formLoading">
      <el-form-item label="企业名称" prop="name">
        <el-input v-model="formData.name" placeholder="请输入企业名称" />
      </el-form-item>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="统一社会信用代码" prop="creditCode">
            <el-input v-model="formData.creditCode" placeholder="18 位" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="纳税人识别号" prop="taxNo">
            <el-input v-model="formData.taxNo" placeholder="15-20 位" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="银行账户" prop="bankAccount">
            <el-input v-model="formData.bankAccount" placeholder="16-19 位" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="开户行名称" prop="bankName">
            <el-input v-model="formData.bankName" placeholder="如：中国工商银行北京分行" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="企业地址" prop="address">
        <el-input v-model="formData.address" placeholder="请输入企业地址" />
      </el-form-item>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="企业电话" prop="telephone">
            <el-input v-model="formData.telephone" placeholder="如：010-12345678" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="纳税人类型" prop="taxpayerType">
            <el-select v-model="formData.taxpayerType" class="w-full">
              <el-option label="一般纳税人" value="01" />
              <el-option label="小规模纳税人" value="02" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="联系人姓名" prop="contactName">
            <el-input v-model="formData.contactName" placeholder="选填" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="联系人手机号" prop="contactMobile">
            <el-input v-model="formData.contactMobile" placeholder="选填" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="业务类型" prop="businessType">
            <el-select v-model="formData.businessType" class="w-full">
              <el-option label="再生资源" value="RECYCLE" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="审核状态" prop="status">
            <el-select v-model="formData.status" class="w-full">
              <el-option label="待审核" :value="0" />
              <el-option label="审核通过" :value="1" />
              <el-option label="审核拒绝" :value="2" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
    <template #footer>
      <el-button @click="submitForm" type="primary" :disabled="formLoading">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import { PayerApi, PayerVO } from '@/api/icbc/payer'

defineOptions({ name: 'IcbcPayerForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formData = ref<PayerVO>(buildEmpty())
const formRef = ref()

function buildEmpty(): PayerVO {
  return {
    id: undefined,
    partnerPayerId: undefined,
    name: undefined,
    creditCode: undefined,
    taxNo: undefined,
    bankAccount: undefined,
    bankName: undefined,
    address: undefined,
    telephone: undefined,
    contactName: undefined,
    contactMobile: undefined,
    taxpayerType: '01',
    businessType: 'RECYCLE',
    status: 0
  }
}

const formRules = reactive({
  name: [{ required: true, message: '企业名称不能为空', trigger: 'blur' }],
  creditCode: [
    { required: true, message: '统一社会信用代码不能为空', trigger: 'blur' },
    { pattern: /^[0-9A-Z]{18}$/, message: '统一社会信用代码格式不正确', trigger: 'blur' }
  ],
  taxNo: [
    { required: true, message: '纳税人识别号不能为空', trigger: 'blur' },
    { pattern: /^[0-9A-Z]{15,20}$/, message: '纳税人识别号格式不正确', trigger: 'blur' }
  ],
  bankAccount: [{ pattern: /^\d{16,19}$/, message: '银行账户格式不正确', trigger: 'blur' }],
  telephone: [{ pattern: /^\d{3,4}-?\d{7,8}$/, message: '企业电话格式不正确', trigger: 'blur' }],
  contactMobile: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }]
})

const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await PayerApi.getPayer(id)
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
    const data = formData.value
    if (formType.value === 'create') {
      await PayerApi.createPayer(data)
      message.success(t('common.createSuccess'))
    } else {
      await PayerApi.updatePayer(data)
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
