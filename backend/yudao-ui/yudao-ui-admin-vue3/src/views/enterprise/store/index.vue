<template>
  <ContentWrap>
    <!-- 搜索工作栏 -->
    <el-form
      class="-mb-15px"
      :model="queryParams"
      ref="queryFormRef"
      :inline="true"
      label-width="68px"
    >
      <el-form-item label="企业" prop="enterpriseId">
        <el-select
          v-model="queryParams.enterpriseId"
          placeholder="请选择企业"
          clearable
          filterable
          @keyup.enter="handleQuery"
          class="!w-240px"
        >
          <el-option
            v-for="item in enterpriseListCache"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="上级门店" prop="parentId">
        <el-select
          v-model="queryParams.parentId"
          placeholder="请选择上级门店"
          clearable
          filterable
          @keyup.enter="handleQuery"
          class="!w-240px"
        >
          <el-option
            v-for="item in storeListCache"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="门店名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入门店名称"
          clearable
          @keyup.enter="handleQuery"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item label="门店编码" prop="storeCode">
        <el-input
          v-model="queryParams.storeCode"
          placeholder="请输入门店编码"
          clearable
          @keyup.enter="handleQuery"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item label="联系电话" prop="contactPhone">
        <el-input
          v-model="queryParams.contactPhone"
          placeholder="请输入联系人电话"
          clearable
          @keyup.enter="handleQuery"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select
          v-model="queryParams.status"
          placeholder="请选择状态"
          clearable
          class="!w-240px"
        >
          <el-option
            v-for="dict in getIntDictOptions(DICT_TYPE.ENTERPRISE_STORE_STATUS)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="创建时间" prop="createTime">
        <el-date-picker
          v-model="queryParams.createTime"
          value-format="YYYY-MM-DD HH:mm:ss"
          type="daterange"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          :default-time="[new Date('1 00:00:00'), new Date('1 23:59:59')]"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button
          type="primary"
          plain
          @click="openForm('create')"
          v-hasPermi="['enterprise:store:create']"
        >
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 列表 -->
  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="ID" align="center" prop="id" width="80" />
      <el-table-column label="门店名称" align="center" prop="name" min-width="140px" />
      <el-table-column label="门店编码" align="center" prop="storeCode" width="120px" />
      <el-table-column label="所属企业" align="center" min-width="140px">
        <template #default="scope">
          {{ scope.row.enterpriseName || enterpriseIdToName(scope.row.enterpriseId) }}
        </template>
      </el-table-column>
      <el-table-column label="上级门店" align="center" min-width="140px">
        <template #default="scope">
          <span v-if="scope.row.parentId && scope.row.parentId !== 0">
            {{ scope.row.parentName || storeIdToName(scope.row.parentId) }}
          </span>
          <span v-else>无</span>
        </template>
      </el-table-column>
      <el-table-column label="联系电话" align="center" prop="contactPhone" width="120px" />
      <el-table-column label="状态" align="center" prop="status" width="100px">
        <template #default="scope">
          <el-switch
            v-model="scope.row.status"
            :active-value="1"
            :inactive-value="0"
            @change="(value) => handleStatusChange(scope.row.id, value)"
            v-hasPermi="['enterprise:store:update']"
          />
          <DictTag :type="DICT_TYPE.ENTERPRISE_STORE_STATUS" :value="scope.row.status || 0" class="ml-5px" />
        </template>
      </el-table-column>
      <el-table-column
        label="创建时间"
        align="center"
        prop="createTime"
        :formatter="dateFormatter"
        width="160px"
      />
      <el-table-column label="操作" align="center" fixed="right" width="160px">
        <template #default="scope">
          <el-button
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
            v-hasPermi="['enterprise:store:update']"
          >
            编辑
          </el-button>
          <el-button
            link
            type="danger"
            @click="handleDelete(scope.row.id)"
            v-hasPermi="['enterprise:store:delete']"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <!-- 分页 -->
    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </ContentWrap>

  <!-- 表单弹窗：添加/修改 -->
  <StoreForm ref="formRef" @success="getList" />
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getIntDictOptions, DICT_TYPE } from '@/utils/dict'
import { dateFormatter } from '@/utils/formatTime'
import { StoreApi, StoreVO, EnterpriseStoreUpdateStatusReqVO } from '@/api/enterprise/store'
import StoreForm from './StoreForm.vue'

/** 企业门店 列表 */
defineOptions({ name: 'EnterpriseStore' })

const message = useMessage()
const { t } = useI18n()

const loading = ref(true)
const list = ref<StoreVO[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  enterpriseId: undefined,
  parentId: undefined,
  name: undefined,
  storeCode: undefined,
  contactPhone: undefined,
  status: undefined,
  createTime: []
})
const queryFormRef = ref()
const formRef = ref()

// 用于前端转换企业ID和门店ID为名称
const enterpriseListCache = ref<{ id: number; name: string }[]>([])
const storeListCache = ref<{ id: number; name: string }[]>([])

/** 获取企业列表 (用于下拉和名称转换) */
const fetchEnterpriseList = async () => {
  try {
    const data = await StoreApi.getEnterpriseSimpleList()
    enterpriseListCache.value = data
  } catch (error) {
    console.error('获取企业列表失败', error)
    message.error('获取企业列表失败')
  }
}

/** 获取所有门店列表 (用于下拉和名称转换) */
const fetchAllStores = async () => {
  try {
    const data = await StoreApi.getStoreTree()
    storeListCache.value = data
  } catch (error) {
    console.error('获取门店列表失败', error)
    message.error('获取门店列表失败')
  }
}

/** 企业ID转企业名称 */
const enterpriseIdToName = (enterpriseId?: number): string => {
  if (!enterpriseId) return '-'
  const enterprise = enterpriseListCache.value.find(e => e.id === enterpriseId)
  return enterprise ? enterprise.name : String(enterpriseId)
}

/** 门店ID转门店名称 */
const storeIdToName = (storeId?: number): string => {
  if (!storeId) return '-'
  const store = storeListCache.value.find(s => s.id === storeId)
  return store ? store.name : String(storeId)
}

/** 查询列表 */
const getList = async () => {
  loading.value = true
  try {
    const data = await StoreApi.getStorePage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

/** 搜索按钮操作 */
const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

/** 重置按钮操作 */
const resetQuery = () => {
  queryFormRef.value.resetFields()
  handleQuery()
}

/** 添加/修改操作 */
const openForm = (type: string, id?: number) => {
  formRef.value.open(type, id)
}

/** 修改状态操作 */
const handleStatusChange = async (id: number, status: number) => {
  try {
    const data: EnterpriseStoreUpdateStatusReqVO = { id, status }
    await StoreApi.updateStoreStatus(data)
    message.success(`修改状态成功`)
    await getList() // 重新获取列表
  } catch (error) {
    console.error('修改状态失败', error)
    message.error('修改状态失败')
    await getList() // 重新获取列表以恢复原状态
  }
}

/** 删除按钮操作 */
const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await StoreApi.deleteStore(id)
    message.success(t('common.delSuccess'))
    await getList()
  } catch (error) {
    console.error('删除失败', error)
  }
}

/** 初始化 **/
onMounted(async () => {
  // 并行加载企业列表和门店列表
  await Promise.all([fetchEnterpriseList(), fetchAllStores()])
  // 然后获取门店分页数据
  await getList()
})
</script> 