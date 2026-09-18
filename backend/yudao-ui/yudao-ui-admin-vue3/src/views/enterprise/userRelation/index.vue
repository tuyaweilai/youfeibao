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
      <el-form-item label="用户" prop="userId">
        <el-select
          v-model="queryParams.userId"
          placeholder="请选择用户"
          clearable
          filterable
          remote
          :remote-method="handleUserSearch"
          :loading="userSearchLoading"
          @keyup.enter="handleQuery"
          class="!w-240px"
        >
          <el-option
            v-for="item in userSearchList"
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
      <el-form-item label="门店" prop="storeId">
        <el-select
          v-model="queryParams.storeId"
          placeholder="请选择门店"
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
      <el-form-item label="关系类型" prop="relationType">
        <el-select
          v-model="queryParams.relationType"
          placeholder="请选择关系类型"
          clearable
          class="!w-240px"
        >
          <el-option
            v-for="dict in getIntDictOptions(DICT_TYPE.ENTERPRISE_RELATION_TYPE)"
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
          v-hasPermi="['enterprise:user:manage']"
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
      <el-table-column label="用户名称" align="center" min-width="140px">
        <template #default="scope">
          <div class="flex flex-col items-center">
            <span>{{ scope.row.nickname || scope.row.username || userIdToName(scope.row.userId) }}</span>
            <span v-if="scope.row.username" class="text-gray-400 text-xs">{{ scope.row.username }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="企业名称" align="center" min-width="140px">
        <template #default="scope">
          {{ scope.row.enterpriseName || enterpriseIdToName(scope.row.enterpriseId) }}
        </template>
      </el-table-column>
      <el-table-column label="门店名称" align="center" min-width="140px">
        <template #default="scope">
          <span v-if="scope.row.storeId">
            {{ scope.row.storeName || storeIdToName(scope.row.storeId) }}
          </span>
          <span v-else>无</span>
        </template>
      </el-table-column>
      <el-table-column label="关系类型" align="center" width="120px">
        <template #default="scope">
          <DictTag
            :type="DICT_TYPE.ENTERPRISE_RELATION_TYPE"
            :value="scope.row.relationType || 0"
          />
        </template>
      </el-table-column>
      <el-table-column label="主联系人" align="center" width="80px">
        <template #default="scope">
          <el-tag v-if="scope.row.isPrimaryContact" type="success" size="small">是</el-tag>
          <el-tag v-else type="info" size="small">否</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="默认企业" align="center" width="80px">
        <template #default="scope">
          <el-tag v-if="scope.row.isDefaultEnterprise" type="success" size="small">是</el-tag>
          <el-button
            v-else
            link
            type="primary"
            size="small"
            @click="handleSetDefault(scope.row)"
            v-hasPermi="['enterprise:user:manage']"
          >
            设为默认
          </el-button>
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
            v-hasPermi="['enterprise:user:manage']"
          >
            编辑
          </el-button>
          <el-button
            link
            type="danger"
            @click="handleDelete(scope.row.id)"
            v-hasPermi="['enterprise:user:manage']"
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
  <UserRelationForm ref="formRef" @success="getList" />
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getIntDictOptions, DICT_TYPE } from '@/utils/dict'
import { dateFormatter } from '@/utils/formatTime'
import { UserRelationApi, UserRelationVO, EnterpriseUserRelationSetDefaultReqVO } from '@/api/enterprise/userRelation'
import UserRelationForm from './UserRelationForm.vue'

/** 企业用户关系 列表 */
defineOptions({ name: 'EnterpriseUserRelation' })

const message = useMessage()
const { t } = useI18n()

const loading = ref(true)
const list = ref<UserRelationVO[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  userId: undefined,
  enterpriseId: undefined,
  storeId: undefined,
  relationType: undefined,
  createTime: []
})
const queryFormRef = ref()
const formRef = ref()

// 用于前端转换ID为名称
const userSearchList = ref<{ id: number; username: string; nickname?: string }[]>([])
const userSearchLoading = ref(false)
const userCacheMap = ref<Map<number, { username: string; nickname?: string }>>(new Map())
const enterpriseListCache = ref<{ id: number; name: string }[]>([])
const storeListCache = ref<{ id: number; name: string }[]>([])

/** 搜索用户 */
const handleUserSearch = async (keyword: string) => {
  if (keyword.trim().length === 0) return
  
  userSearchLoading.value = true
  try {
    const data = await UserRelationApi.getUserSimpleList(keyword)
    userSearchList.value = data
    
    // 更新用户缓存
    data.forEach(user => {
      userCacheMap.value.set(user.id, { username: user.username, nickname: user.nickname })
    })
  } catch (error) {
    console.error('搜索用户失败', error)
  } finally {
    userSearchLoading.value = false
  }
}

/** 获取企业列表 */
const fetchEnterpriseList = async () => {
  try {
    const data = await UserRelationApi.getEnterpriseSimpleList()
    enterpriseListCache.value = data
  } catch (error) {
    console.error('获取企业列表失败', error)
    message.error('获取企业列表失败')
  }
}

/** 获取所有门店列表 */
const fetchAllStores = async () => {
  try {
    const data = await UserRelationApi.getStoreSimpleList()
    storeListCache.value = data
  } catch (error) {
    console.error('获取门店列表失败', error)
    message.error('获取门店列表失败')
  }
}

/** 用户ID转用户名称 */
const userIdToName = (userId?: number): string => {
  if (!userId) return '-'
  const user = userCacheMap.value.get(userId)
  return user ? (user.nickname || user.username) : String(userId)
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
    const data = await UserRelationApi.getUserRelationPage(queryParams)
    list.value = data.list
    total.value = data.total
    
    // 更新用户缓存
    const userIds = list.value.map(item => item.userId).filter(Boolean)
    if (userIds.length > 0) {
      const userNotInCache = userIds.filter(id => !userCacheMap.value.has(id))
      if (userNotInCache.length > 0) {
        // 这里可以批量查询未缓存的用户信息，但由于没有提供批量查询的API，暂不实现
        // 最好的情况是后端直接返回用户名称，前端不需要查询
      }
    }
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

/** 设置默认企业操作 */
const handleSetDefault = async (row: UserRelationVO) => {
  try {
    const data: EnterpriseUserRelationSetDefaultReqVO = {
      userId: row.userId,
      relationId: row.id
    }
    await UserRelationApi.setDefaultEnterprise(data)
    message.success('设置默认企业成功')
    await getList() // 重新获取列表
  } catch (error) {
    console.error('设置默认企业失败', error)
    message.error('设置默认企业失败')
  }
}

/** 删除按钮操作 */
const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await UserRelationApi.deleteUserRelation(id)
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
  // 然后获取用户企业关系分页数据
  await getList()
})
</script> 