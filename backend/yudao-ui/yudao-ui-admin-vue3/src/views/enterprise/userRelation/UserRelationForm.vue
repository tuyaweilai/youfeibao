<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible" width="600px">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="120px"
      v-loading="formLoading"
    >
      <el-form-item label="用户" prop="userId">
        <el-select
          v-model="formData.userId"
          placeholder="请选择用户"
          class="w-full"
          filterable
          remote
          :remote-method="handleUserSearch"
          :loading="userLoading"
        >
          <el-option
            v-for="item in userList"
            :key="item.id"
            :label="item.nickname || item.username"
            :value="item.id"
          >
            <div class="flex items-center">
              <span>{{ item.nickname || item.username }}</span>
              <span class="text-gray-400 ml-2">({{ item.username }})</span>
            </div>
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="企业" prop="enterpriseId">
        <el-select
          v-model="formData.enterpriseId"
          placeholder="请选择企业"
          class="w-full"
          filterable
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
      <el-form-item label="门店" prop="storeId">
        <el-select
          v-model="formData.storeId"
          placeholder="请选择门店"
          class="w-full"
          filterable
          clearable
        >
          <el-option label="无门店" :value="0" />
          <el-option
            v-for="item in storeList"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="关系类型" prop="relationType">
        <el-select v-model="formData.relationType" placeholder="请选择关系类型" class="w-full">
          <el-option
            v-for="dict in getIntDictOptions(DICT_TYPE.ENTERPRISE_RELATION_TYPE)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="是否主联系人" prop="isPrimaryContact">
        <el-radio-group v-model="formData.isPrimaryContact">
          <el-radio :label="true">是</el-radio>
          <el-radio :label="false">否</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="是否默认企业" prop="isDefaultEnterprise">
        <el-radio-group v-model="formData.isDefaultEnterprise">
          <el-radio :label="true">是</el-radio>
          <el-radio :label="false">否</el-radio>
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
import { UserRelationApi, EnterpriseUserRelationBaseVO } from '@/api/enterprise/userRelation'
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'

defineOptions({ name: 'EnterpriseUserRelationForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('') // 'create' | 'update'

const formData = ref<EnterpriseUserRelationBaseVO>({
  userId: undefined!,
  enterpriseId: undefined!,
  storeId: undefined,
  relationType: undefined!,
  isPrimaryContact: false,
  isDefaultEnterprise: false
})

const formRules = reactive({
  userId: [{ required: true, message: '用户不能为空', trigger: 'change' }],
  enterpriseId: [{ required: true, message: '企业不能为空', trigger: 'change' }],
  relationType: [{ required: true, message: '关系类型不能为空', trigger: 'change' }]
})

const formRef = ref()
const userList = ref<{ id: number; username: string; nickname?: string }[]>([])
const userLoading = ref(false)
const enterpriseList = ref<{ id: number; name: string }[]>([])
const storeList = ref<{ id: number; name: string }[]>([])

/** 搜索用户 */
const handleUserSearch = async (keyword: string) => {
  if (keyword.trim().length === 0) return
  
  userLoading.value = true
  try {
    const data = await UserRelationApi.getUserSimpleList(keyword)
    userList.value = data
  } catch (error) {
    console.error('搜索用户失败', error)
  } finally {
    userLoading.value = false
  }
}

/** 获取企业列表 */
const fetchEnterpriseList = async () => {
  try {
    const data = await UserRelationApi.getEnterpriseSimpleList()
    enterpriseList.value = data
  } catch (error) {
    console.error('获取企业列表失败', error)
    message.error('获取企业列表失败')
  }
}

/** 根据企业ID获取门店列表 */
const fetchStoreList = async (enterpriseId?: number) => {
  if (!enterpriseId) {
    storeList.value = []
    return
  }
  
  try {
    const data = await UserRelationApi.getStoreSimpleList(enterpriseId)
    storeList.value = data
  } catch (error) {
    console.error('获取门店列表失败', error)
    message.error('获取门店列表失败')
  }
}

/** 处理企业变更 */
const handleEnterpriseChange = (value: number) => {
  formData.value.storeId = undefined // 清空门店选择
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
      formData.value = await UserRelationApi.getUserRelation(id)
      
      // 获取相关联数据
      // 1. 如果有用户ID，获取用户信息
      if (formData.value.userId) {
        const userInfo = await UserRelationApi.getUserSimpleList('')
        userList.value = userInfo.filter(user => user.id === formData.value.userId) 
          || userInfo.slice(0, 10) // 如果没有匹配，显示前10个
      }
      
      // 2. 如果有企业ID，获取该企业的门店列表
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
    const data = { ...formData.value } as EnterpriseUserRelationBaseVO
    
    // 如果没有选择门店，设置为undefined
    if (!data.storeId || data.storeId === 0) {
      data.storeId = undefined
    }
    
    if (formType.value === 'create') {
      await UserRelationApi.createUserRelation(data)
      message.success(t('common.createSuccess'))
    } else {
      await UserRelationApi.updateUserRelation(data)
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
    userId: undefined!,
    enterpriseId: undefined!,
    storeId: undefined,
    relationType: undefined!,
    isPrimaryContact: false,
    isDefaultEnterprise: false
  }
  formRef.value?.resetFields()
  storeList.value = []
}
</script> 