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
      <el-form-item label="企业ID" prop="enterpriseId">
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
      <el-form-item label="资质名称" prop="qualificationName">
        <el-input
          v-model="queryParams.qualificationName"
          placeholder="请输入资质名称"
          clearable
          @keyup.enter="handleQuery"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item label="资质类型" prop="qualificationType">
        <el-select
          v-model="queryParams.qualificationType"
          placeholder="请选择资质类型"
          clearable
          class="!w-240px"
        >
          <el-option
            v-for="dict in getIntDictOptions(DICT_TYPE.ENTERPRISE_QUALIFICATION_TYPE)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="资质编号" prop="qualificationCode">
        <el-input
          v-model="queryParams.qualificationCode"
          placeholder="请输入资质编号"
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
            v-for="dict in getIntDictOptions(DICT_TYPE.ENTERPRISE_QUALIFICATION_STATUS)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="到期日期" prop="expiryDate">
        <el-date-picker
          v-model="queryParams.expiryDate"
          value-format="YYYY-MM-DD HH:mm:ss" 
          type="daterange"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          :default-time="[new Date('1 00:00:00'), new Date('1 23:59:59')]"
          class="!w-240px"
        />
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
          v-hasPermi="['enterprise:cert:create']"
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
      <el-table-column label="企业名称" align="center" prop="enterpriseName" min-width="160px">
         <template #default="scope">
          {{ scope.row.enterpriseName || enterpriseIdToName(scope.row.enterpriseId) }}
        </template>
      </el-table-column>
      <el-table-column label="资质名称" align="center" prop="qualificationName" min-width="140px" />
      <el-table-column label="资质类型" align="center" prop="qualificationType" width="120px">
        <template #default="scope">
          <DictTag :type="DICT_TYPE.ENTERPRISE_QUALIFICATION_TYPE" :value="scope.row.qualificationType || 0" />
        </template>
      </el-table-column>
      <el-table-column label="资质编号" align="center" prop="qualificationCode" min-width="140px" />
      <el-table-column
        label="发证日期"
        align="center"
        prop="issueDate"
        :formatter="dateFormatter2" 
        width="120px"
      />
      <el-table-column
        label="到期日期"
        align="center"
        prop="expiryDate"
        :formatter="dateFormatter2"
        width="120px"
      />
      <el-table-column label="状态" align="center" prop="status" width="100px">
        <template #default="scope">
          <DictTag :type="DICT_TYPE.ENTERPRISE_QUALIFICATION_STATUS" :value="scope.row.status || 0" />
        </template>
      </el-table-column>
      <el-table-column
        label="创建时间"
        align="center"
        prop="createTime"
        :formatter="dateFormatter"
        width="160px"
      />
      <el-table-column label="操作" align="center" fixed="right" width="220px">
        <template #default="scope">
          <el-button
            link
            type="primary"
            @click="handleDetail(scope.row)"
            v-hasPermi="['enterprise:cert:query']"
          >
            详情
          </el-button>
          <el-button
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
            v-hasPermi="['enterprise:cert:update']"
          >
            编辑
          </el-button>
          <el-button
            link
            type="primary"
            @click="openAuditForm(scope.row.id)"
            v-hasPermi="['enterprise:cert:audit']"
          >
            审核
          </el-button>
          <el-button
            link
            type="danger"
            @click="handleDelete(scope.row.id)"
            v-hasPermi="['enterprise:cert:delete']"
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
  <QualificationForm ref="formRef" @success="getList" />
  <!-- 审核弹窗 -->
  <QualificationAuditForm ref="auditFormRef" @success="getList" />

  <!-- 详情弹窗 -->
  <el-dialog title="企业资质详情" v-model="detailVisible" width="700px">
    <el-descriptions :column="2" border>
      <el-descriptions-item label="企业名称">
        {{ detailData?.enterpriseName || enterpriseIdToName(detailData?.enterpriseId) }}
      </el-descriptions-item>
      <el-descriptions-item label="资质名称">{{ detailData?.qualificationName }}</el-descriptions-item>
      <el-descriptions-item label="资质类型">
        <DictTag :type="DICT_TYPE.ENTERPRISE_QUALIFICATION_TYPE" :value="detailData?.qualificationType || 0" />
      </el-descriptions-item>
      <el-descriptions-item label="资质编号">{{ detailData?.qualificationCode }}</el-descriptions-item>
      <el-descriptions-item label="发证日期">{{ detailData?.issueDate }}</el-descriptions-item>
      <el-descriptions-item label="到期日期">{{ detailData?.expiryDate }}</el-descriptions-item>
      <el-descriptions-item label="发证机关">{{ detailData?.issuingAuthority }}</el-descriptions-item>
      <el-descriptions-item label="状态">
        <DictTag :type="DICT_TYPE.ENTERPRISE_QUALIFICATION_STATUS" :value="detailData?.status || 0" />
      </el-descriptions-item>
      <el-descriptions-item label="创建时间">{{ detailData?.createTime ? formatDate(new Date(detailData.createTime)) : '' }}</el-descriptions-item>
      <el-descriptions-item label="资质文件">
        <el-link 
          type="primary" 
          :underline="false"
          v-if="detailData?.qualificationFileId" 
          @click="handleViewFile(detailData?.qualificationFileId)"
        >
          查看文件
        </el-link>
        <span v-else>无</span>
      </el-descriptions-item>
    </el-descriptions>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { QualificationApi, QualificationVO } from '@/api/enterprise/qualification'
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { dateFormatter, dateFormatter2, formatDate } from '@/utils/formatTime'
import QualificationForm from './QualificationForm.vue'
import QualificationAuditForm from './QualificationAuditForm.vue'
// import { InfoApi } from '@/api/enterprise/info' // 用于获取企业列表

defineOptions({ name: 'EnterpriseQualification' })

const message = useMessage()
const { t } = useI18n()

const loading = ref(true)
const list = ref<QualificationVO[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  enterpriseId: undefined,
  qualificationName: undefined,
  qualificationType: undefined,
  qualificationCode: undefined,
  status: undefined,
  expiryDate: [],
  createTime: []
})
const queryFormRef = ref()
const formRef = ref()
const auditFormRef = ref()

// 用于前端转换 enterpriseId 为 enterpriseName (如果后端不直接提供)
const enterpriseListCache = ref<{ id: number; name: string }[]>([])

/** 获取企业列表 (用于名称转换) */
const fetchEnterpriseListForMapping = async () => {
  try {
    // 尝试使用企业信息API获取列表
    // 如果还没有实际API，则使用模拟数据
    // 如果有InfoApi，取消下面的注释并替换模拟数据
    // const data = await InfoApi.getEnterpriseSimpleList() 
    const data = await QualificationApi.getEnterpriseSimpleList()
    enterpriseListCache.value = data
  } catch (error) {
    console.error('获取企业列表用于映射失败', error)
    message.error('获取企业列表失败')
  }
}

const enterpriseIdToName = (enterpriseId?: number): string => {
  if (!enterpriseId) return '-'
  const enterprise = enterpriseListCache.value.find(e => e.id === enterpriseId)
  return enterprise ? enterprise.name : String(enterpriseId) // 如果找不到，返回ID
}

/** 查询列表 */
const getList = async () => {
  loading.value = true
  try {
    const data = await QualificationApi.getQualificationPage(queryParams)
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

/** 审核操作 */
const openAuditForm = (id: number) => {
  auditFormRef.value.open(id)
}

/** 删除按钮操作 */
const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await QualificationApi.deleteQualification(id)
    message.success(t('common.delSuccess'))
    getList()
  } catch (error) {
    console.error('删除失败', error)
  }
}

// 详情弹窗
const detailVisible = ref(false)
const detailData = ref<QualificationVO | null>(null)

/** 查看详情 */
const handleDetail = (row: QualificationVO) => {
  detailVisible.value = true
  detailData.value = row
}

/** 查看文件 */
const handleViewFile = (fileId: number) => {
  if (!fileId) return
  // 假设您有一个通用的文件预览或下载工具
  // 如果没有，可以通过构建URL进行下载或预览
  const downloadUrl = import.meta.env.VITE_APP_BASE_API + '/infra/file/download?configId=0&path=' + fileId
  window.open(downloadUrl)
  // 或者使用项目中的下载工具，例如：
  // download.download(downloadUrl)
}

/** 初始化 **/
onMounted(async () => {
  // 先加载企业列表，用于搜索下拉和名称映射
  await fetchEnterpriseListForMapping()
  // 然后获取资质列表
  await getList()
})
</script> 