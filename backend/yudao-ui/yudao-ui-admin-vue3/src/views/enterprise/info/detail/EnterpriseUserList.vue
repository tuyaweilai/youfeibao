<template>
  <div v-loading="loading">
    <el-table :data="list" :stripe="true">
      <el-table-column type="index" width="50" />
      <el-table-column label="用户名称" align="center" min-width="140">
        <template #default="scope">
          <div class="flex flex-col items-center">
            <span>{{ scope.row.nickname || scope.row.username }}</span>
            <span v-if="scope.row.username" class="text-gray-400 text-xs">{{ scope.row.username }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="门店名称" align="center" min-width="140">
        <template #default="scope">
          <span v-if="scope.row.storeId">
            {{ scope.row.storeName || '门店' + scope.row.storeId }}
          </span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="关系类型" align="center" width="120">
        <template #default="scope">
          <dict-tag
            :type="DICT_TYPE.ENTERPRISE_RELATION_TYPE"
            :value="scope.row.relationType || 0"
          />
        </template>
      </el-table-column>
      <el-table-column label="主联系人" align="center" width="100">
        <template #default="scope">
          <el-tag v-if="scope.row.isPrimaryContact" type="success" size="small">是</el-tag>
          <el-tag v-else type="info" size="small">否</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="默认企业" align="center" width="100">
        <template #default="scope">
          <el-tag v-if="scope.row.isDefaultEnterprise" type="success" size="small">是</el-tag>
          <el-tag v-else type="info" size="small">否</el-tag>
        </template>
      </el-table-column>
      <el-table-column
        label="创建时间"
        align="center"
        prop="createTime"
        width="160"
      >
        <template #default="scope">
          {{ scope.row.createTime ? formatDate(scope.row.createTime) : '-' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="100" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="handleViewUser(scope.row.userId)" v-hasPermi="['system:user:query']">查看用户</el-button>
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, PropType, watchEffect } from 'vue'
import { UserRelationApi, UserRelationVO } from '@/api/enterprise/userRelation'
import { getIntDictOptions, DICT_TYPE } from '@/utils/dict'
import { dateFormatter, formatDate } from '@/utils/formatTime'
import { useRouter } from 'vue-router'

defineOptions({ name: 'EnterpriseUserList' })

// 接收企业ID属性
const props = defineProps({
  enterpriseId: {
    type: Number as PropType<number>,
    required: true
  }
})

const loading = ref(false)
const list = ref<UserRelationVO[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  enterpriseId: props.enterpriseId
})

const router = useRouter()

// 获取企业关联用户列表
const getList = async () => {
  loading.value = true
  try {
    const params = {
      ...queryParams,
      enterpriseId: props.enterpriseId // 确保使用最新的enterpriseId
    }
    const { list: records, total: totalCount } = await UserRelationApi.getUserRelationPage(params)
    list.value = records
    total.value = totalCount
  } finally {
    loading.value = false
  }
}

// 查看用户详情
const handleViewUser = (userId: number) => {
  if (userId) {
    router.push({ path: `/member/user/detail/${userId}` })
  }
}

// 监听企业ID变化
watchEffect(() => {
  if (props.enterpriseId) {
    queryParams.enterpriseId = props.enterpriseId
    getList()
  }
})
</script> 