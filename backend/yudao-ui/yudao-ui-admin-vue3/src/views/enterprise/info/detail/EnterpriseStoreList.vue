<template>
  <div v-loading="loading">
    <el-table :data="list" :stripe="true">
      <el-table-column type="index" width="50" />
      <el-table-column label="门店名称" align="center" prop="name" min-width="120" />
      <el-table-column label="门店编码" align="center" prop="storeCode" width="120" />
      <el-table-column label="上级门店" align="center" min-width="120">
        <template #default="scope">
          {{ scope.row.parentName || (scope.row.parentId ? '门店' + scope.row.parentId : '-') }}
        </template>
      </el-table-column>
      <el-table-column label="联系人" align="center" prop="contactName" width="100" />
      <el-table-column label="联系电话" align="center" prop="contactPhone" width="120" />
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template #default="scope">
          <dict-tag :type="DICT_TYPE.ENTERPRISE_STORE_STATUS" :value="scope.row.status || 0" />
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
          <el-button link type="primary" @click="handleView(scope.row)" v-hasPermi="['enterprise:store:query']">查看</el-button>
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

    <!-- 查看门店弹窗 -->
    <el-dialog v-model="dialogVisible" title="查看门店详情" width="600px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="门店名称">{{ detailData.name }}</el-descriptions-item>
        <el-descriptions-item label="门店编码">{{ detailData.storeCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="上级门店">
          {{ detailData.parentName || (detailData.parentId ? '门店' + detailData.parentId : '-') }}
        </el-descriptions-item>
        <el-descriptions-item label="地址" :span="2">
          {{ detailData.addressDetail || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="联系人">{{ detailData.contactName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ detailData.contactPhone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <dict-tag :type="DICT_TYPE.ENTERPRISE_STORE_STATUS" :value="detailData.status || 0" />
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">
          {{ detailData.createTime ? formatDate(detailData.createTime) : '-' }}
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, PropType, watchEffect } from 'vue'
import { StoreApi, StoreVO } from '@/api/enterprise/store'
import { getIntDictOptions, DICT_TYPE } from '@/utils/dict'
import { dateFormatter, formatDate } from '@/utils/formatTime'

defineOptions({ name: 'EnterpriseStoreList' })

// 接收企业ID属性
const props = defineProps({
  enterpriseId: {
    type: Number as PropType<number>,
    required: true
  }
})

const loading = ref(false)
const list = ref<StoreVO[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  enterpriseId: props.enterpriseId
})

// 详情弹窗
const dialogVisible = ref(false)
const detailData = ref<StoreVO>({} as StoreVO)

// 获取企业门店列表
const getList = async () => {
  loading.value = true
  try {
    const params = {
      ...queryParams,
      enterpriseId: props.enterpriseId // 确保使用最新的enterpriseId
    }
    const { list: records, total: totalCount } = await StoreApi.getStorePage(params)
    list.value = records
    total.value = totalCount
  } finally {
    loading.value = false
  }
}

// 查看门店详情
const handleView = (row: StoreVO) => {
  detailData.value = { ...row }
  dialogVisible.value = true
}

// 监听企业ID变化
watchEffect(() => {
  if (props.enterpriseId) {
    queryParams.enterpriseId = props.enterpriseId
    getList()
  }
})
</script> 