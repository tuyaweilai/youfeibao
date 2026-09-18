<template>
  <div v-loading="loading">
    <el-table :data="list" :stripe="true">
      <el-table-column type="index" width="50" />
      <el-table-column label="资质名称" align="center" prop="qualificationName" min-width="120" />
      <el-table-column label="资质类型" align="center" prop="qualificationType" width="120">
        <template #default="scope">
          <dict-tag :type="DICT_TYPE.ENTERPRISE_QUALIFICATION_TYPE" :value="scope.row.qualificationType || 0" />
        </template>
      </el-table-column>
      <el-table-column label="资质编号" align="center" prop="qualificationCode" min-width="120" />
      <el-table-column label="发证日期" align="center" prop="issueDate" width="120">
        <template #default="scope">
          {{ scope.row.issueDate ? formatDate(scope.row.issueDate, 'YYYY-MM-DD') : '-' }}
        </template>
      </el-table-column>
      <el-table-column label="到期日期" align="center" prop="expiryDate" width="120">
        <template #default="scope">
          {{ scope.row.expiryDate ? formatDate(scope.row.expiryDate, 'YYYY-MM-DD') : '-' }}
        </template>
      </el-table-column>
      <el-table-column label="发证机关" align="center" prop="issuingAuthority" min-width="120" />
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template #default="scope">
          <dict-tag :type="DICT_TYPE.ENTERPRISE_QUALIFICATION_STATUS" :value="scope.row.status || 0" />
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="100" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="handleView(scope.row)" v-hasPermi="['enterprise:qualification:query']">查看</el-button>
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

    <!-- 查看资质弹窗 -->
    <el-dialog v-model="dialogVisible" title="查看资质详情" width="600px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="资质名称">{{ detailData.qualificationName }}</el-descriptions-item>
        <el-descriptions-item label="资质类型">
          <dict-tag :type="DICT_TYPE.ENTERPRISE_QUALIFICATION_TYPE" :value="detailData.qualificationType || 0" />
        </el-descriptions-item>
        <el-descriptions-item label="资质编号">{{ detailData.qualificationCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="发证日期">
          {{ detailData.issueDate ? formatDate(detailData.issueDate, 'YYYY-MM-DD') : '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="到期日期">
          {{ detailData.expiryDate ? formatDate(detailData.expiryDate, 'YYYY-MM-DD') : '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="发证机关">{{ detailData.issuingAuthority || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <dict-tag :type="DICT_TYPE.ENTERPRISE_QUALIFICATION_STATUS" :value="detailData.status || 0" />
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">
          {{ detailData.createTime ? formatDate(detailData.createTime) : '-' }}
        </el-descriptions-item>
      </el-descriptions>
      <div v-if="detailData.qualificationFileId" class="mt-3">
        <p class="mb-2 font-bold">资质文件</p>
        <!-- 这里可以查看资质文件，需要实现文件预览组件 -->
        <el-button size="small" type="primary" @click="previewFile(detailData.qualificationFileId)">
          预览资质文件
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, PropType, watchEffect } from 'vue'
import { QualificationApi, QualificationVO } from '@/api/enterprise/qualification'
import { getIntDictOptions, DICT_TYPE } from '@/utils/dict'
import { dateFormatter, dateFormatter2, formatDate } from '@/utils/formatTime'

defineOptions({ name: 'EnterpriseQualificationList' })

// 接收企业ID属性
const props = defineProps({
  enterpriseId: {
    type: Number as PropType<number>,
    required: true
  }
})

const loading = ref(false)
const list = ref<QualificationVO[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  enterpriseId: props.enterpriseId
})

// 详情弹窗
const dialogVisible = ref(false)
const detailData = ref<QualificationVO>({} as QualificationVO)

// 获取企业资质列表
const getList = async () => {
  loading.value = true
  try {
    const params = {
      ...queryParams,
      enterpriseId: props.enterpriseId // 确保使用最新的enterpriseId
    }
    const { list: records, total: totalCount } = await QualificationApi.getQualificationPage(params)
    list.value = records
    total.value = totalCount
  } finally {
    loading.value = false
  }
}

// 查看资质详情
const handleView = (row: QualificationVO) => {
  detailData.value = { ...row }
  dialogVisible.value = true
}

// 预览文件
const previewFile = (fileId: number) => {
  window.open(`/admin-api/infra/file/get/${fileId}`, '_blank')
}

// 监听企业ID变化
watchEffect(() => {
  if (props.enterpriseId) {
    queryParams.enterpriseId = props.enterpriseId
    getList()
  }
})
</script> 