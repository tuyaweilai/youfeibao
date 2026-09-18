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
        <el-select
          v-model="formData.enterpriseId"
          placeholder="请输入关键字搜索企业"
          class="w-full"
          filterable
          remote
          :remote-method="remoteSearchEnterprise"
          :loading="enterpriseSearchLoading"
          @change="handleEnterpriseChange"
        >
          <el-option
            v-for="item in enterpriseList"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="上级门店" prop="parentId">
        <el-select v-model="formData.parentId" placeholder="请选择上级门店" class="w-full" filterable clearable>
          <el-option label="无上级门店" :value="0" />
          <el-option
            v-for="item in storeList"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="门店名称" prop="name">
        <el-input v-model="formData.name" placeholder="请输入门店名称" />
      </el-form-item>
      <el-form-item label="门店编码" prop="storeCode">
        <el-input v-model="formData.storeCode" placeholder="请输入门店编码" />
      </el-form-item>
      <el-form-item label="门店地址">
        <el-row :gutter="10">
          <el-col :span="8">
            <el-form-item prop="addressProvinceCode" class="!mb-0">
              <el-input v-model="formData.addressProvinceCode" placeholder="省编码" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item prop="addressCityCode" class="!mb-0">
              <el-input v-model="formData.addressCityCode" placeholder="市编码" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item prop="addressDistrictCode" class="!mb-0">
              <el-input v-model="formData.addressDistrictCode" placeholder="区编码" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form-item>
      <el-form-item label="详细地址" prop="addressDetail">
        <el-input v-model="formData.addressDetail" placeholder="请输入详细地址" />
      </el-form-item>
      <el-form-item label="联系人姓名" prop="contactName">
        <el-input v-model="formData.contactName" placeholder="请输入联系人姓名" />
      </el-form-item>
      <el-form-item label="联系人电话" prop="contactPhone">
        <el-input v-model="formData.contactPhone" placeholder="请输入联系人电话" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="formData.status">
          <el-radio
            v-for="dict in getIntDictOptions(DICT_TYPE.ENTERPRISE_STORE_STATUS)"
            :key="dict.value"
            :label="dict.value"
          >
            {{ dict.label }}
          </el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="submitForm" type="primary" :disabled="formLoading">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { StoreApi, EnterpriseStoreBaseVO } from '@/api/enterprise/store'
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'

defineOptions({ name: 'EnterpriseStoreForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('') // 'create' | 'update'

const formData = ref<EnterpriseStoreBaseVO>({
  enterpriseId: undefined!,
  parentId: undefined,
  name: '',
  storeCode: '',
  addressProvinceCode: '',
  addressCityCode: '',
  addressDistrictCode: '',
  addressDetail: '',
  contactName: '',
  contactPhone: '',
  status: 0
})

const formRules = reactive({
  enterpriseId: [{ required: true, message: '企业名称不能为空', trigger: 'change' }],
  name: [
    { required: true, message: '门店名称不能为空', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  storeCode: [
    { max: 30, message: '长度不能超过 30 个字符', trigger: 'blur' }
  ],
  contactPhone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ],
  status: [{ required: true, message: '状态不能为空', trigger: 'change' }]
})

const formRef = ref()
const enterpriseList = ref<{ id: number; name: string }[]>([])
const storeList = ref<{ id: number; name: string }[]>([])
const enterpriseSearchLoading = ref(false)

/** 远程搜索企业 */
const remoteSearchEnterprise = async (query: string) => {
  if (query === '') {
    enterpriseList.value = []
    return
  }
  
  enterpriseSearchLoading.value = true
  try {
    // 如果query为空字符串，获取所有企业
    const data = await StoreApi.getEnterpriseSimpleList()
    
    // 如果有查询关键字，则在前端过滤
    if (query) {
      enterpriseList.value = data.filter(item => 
        item.name.toLowerCase().includes(query.toLowerCase())
      )
    } else {
      enterpriseList.value = data
    }
  } catch (error) {
    console.error('搜索企业失败', error)
    message.error('搜索企业失败')
  } finally {
    enterpriseSearchLoading.value = false
  }
}

/** 获取企业列表 */
const fetchEnterpriseList = async () => {
  try {
    const data = await StoreApi.getEnterpriseSimpleList()
    enterpriseList.value = data
  } catch (error) {
    console.error('获取企业列表失败', error)
    message.error('获取企业列表失败')
  }
}

/** 获取门店列表 (用于选择上级门店) */
const fetchStoreList = async (enterpriseId?: number) => {
  if (!enterpriseId) {
    storeList.value = []
    return
  }
  
  try {
    const data = await StoreApi.getStoreTree(enterpriseId)
    storeList.value = data
  } catch (error) {
    console.error('获取门店列表失败', error)
    message.error('获取门店列表失败')
  }
}

/** 处理企业变更 */
const handleEnterpriseChange = (value: number) => {
  formData.value.parentId = undefined // 清空上级门店选择
  fetchStoreList(value)
}

/** 打开弹窗 */
const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  
  // 获取企业列表
  await fetchEnterpriseList()

  if (id) {
    formLoading.value = true
    try {
      formData.value = await StoreApi.getStore(id)
      
      // 获取当前企业的门店列表
      if (formData.value.enterpriseId) {
        await fetchStoreList(formData.value.enterpriseId)
      }
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
    const data = { ...formData.value } as EnterpriseStoreBaseVO
    
    // 如果没有选择上级门店，设置为0或undefined
    if (!data.parentId) {
      data.parentId = 0
    }
    
    if (formType.value === 'create') {
      await StoreApi.createStore(data)
      message.success(t('common.createSuccess'))
    } else {
      await StoreApi.updateStore(data)
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
    parentId: undefined,
    name: '',
    storeCode: '',
    addressProvinceCode: '',
    addressCityCode: '',
    addressDistrictCode: '',
    addressDetail: '',
    contactName: '',
    contactPhone: '',
    status: 0
  }
  formRef.value?.resetFields()
  storeList.value = []
}
</script> 