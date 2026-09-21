<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="120px"
      v-loading="formLoading"
    >
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="姓名" prop="name">
            <el-input v-model="formData.name" placeholder="请输入姓名" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="身份证号" prop="idCardNo">
            <el-input v-model="formData.idCardNo" placeholder="请输入身份证号" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="手机号" prop="mobile">
            <el-input v-model="formData.mobile" placeholder="请输入手机号" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="职业" prop="occupation">
            <el-select v-model="formData.occupation" placeholder="请选择（工行字典）" class="w-full" clearable>
              <el-option
                v-for="item in ICBC_OCCUPATION_OPTIONS"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="银行卡号" prop="bankCardNo">
        <el-input v-model="formData.bankCardNo" placeholder="请输入收款银行卡号" />
      </el-form-item>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="开户银行" prop="bankName">
            <el-input v-model="formData.bankName" placeholder="如：中国工商银行" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="开户支行" prop="bankBranch">
            <el-input v-model="formData.bankBranch" placeholder="如：北京分行营业部" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="地址" prop="address">
        <el-input v-model="formData.address" placeholder="请输入常用住址" />
      </el-form-item>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="证件签发日期" prop="idSignDate">
            <el-input v-model="formData.idSignDate" placeholder="yyyy-MM-dd" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="证件有效期至" prop="idValidityPeriod">
            <el-input v-model="formData.idValidityPeriod" placeholder="永久有效填 9999-12-30" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="是否本人工行卡" prop="accountCode">
        <el-select v-model="formData.accountCode" class="w-full" clearable placeholder="请选择（建档向导确认页定下来的值）">
          <el-option label="工行卡" value="1" />
          <el-option label="非工行卡" value="0" />
        </el-select>
      </el-form-item>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="业务类型" prop="businessType">
            <el-select v-model="formData.businessType" placeholder="请选择" class="w-full">
              <el-option label="再生资源" value="RECYCLE" />
              <el-option label="报废产品收购" value="SCRAP" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="关联企业" prop="companyName">
            <el-input v-model="formData.companyName" placeholder="选填" />
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
import { ICBC_OCCUPATION_OPTIONS, PayeeApi, PayeeVO } from '@/api/icbc/payee'

/** 出售者档案 表单 */
defineOptions({ name: 'IcbcPayeeForm' })

const { t } = useI18n() // 国际化
const message = useMessage() // 消息弹窗

const dialogVisible = ref(false) // 弹窗的是否展示
const dialogTitle = ref('') // 弹窗的标题
const formLoading = ref(false) // 表单的加载中
const formType = ref('') // 表单的类型：create / update
const formData = ref<PayeeVO>(buildEmpty())
const formRef = ref() // 表单 Ref

function buildEmpty(): PayeeVO {
  return {
    id: undefined,
    partnerPayeeId: undefined,
    name: undefined,
    idCardNo: undefined,
    mobile: undefined,
    bankCardNo: undefined,
    bankName: undefined,
    bankBranch: undefined,
    address: undefined,
    idSignDate: undefined,
    idValidityPeriod: undefined,
    accountCode: undefined,
    businessType: 'RECYCLE',
    occupation: undefined,
    companyName: undefined
  }
}

const formRules = reactive({
  name: [{ required: true, message: '姓名不能为空', trigger: 'blur' }],
  idCardNo: [
    { required: true, message: '身份证号不能为空', trigger: 'blur' },
    {
      pattern: /^[1-9]\d{5}(18|19|20)\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\d{3}[0-9Xx]$/,
      message: '身份证号格式不正确',
      trigger: 'blur'
    }
  ],
  mobile: [
    { required: true, message: '手机号不能为空', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  // 工行收方账号：16-19 位数字（后端 PayeeInfoSaveReqVO 同一条规则）
  bankCardNo: [{ pattern: /^\d{16,19}$/, message: '银行卡号应为 16-19 位数字', trigger: 'blur' }]
})

/** 打开弹窗 */
const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  // 修改时，设置数据
  if (id) {
    formLoading.value = true
    try {
      formData.value = await PayeeApi.getPayee(id)
    } finally {
      formLoading.value = false
    }
  }
}
defineExpose({ open }) // 提供 open 方法，用于打开弹窗

/** 提交表单 */
const emit = defineEmits(['success']) // 定义 success 事件，用于操作成功后的回调
const submitForm = async () => {
  // 校验表单
  await formRef.value.validate()
  // 提交请求
  formLoading.value = true
  try {
    const data = formData.value
    if (formType.value === 'create') {
      await PayeeApi.createPayee(data)
      message.success(t('common.createSuccess'))
    } else {
      await PayeeApi.updatePayee(data)
      message.success(t('common.updateSuccess'))
    }
    dialogVisible.value = false
    // 发送操作成功的事件
    emit('success')
  } finally {
    formLoading.value = false
  }
}

/** 重置表单 */
const resetForm = () => {
  formData.value = buildEmpty()
  formRef.value?.resetFields()
}
</script>
