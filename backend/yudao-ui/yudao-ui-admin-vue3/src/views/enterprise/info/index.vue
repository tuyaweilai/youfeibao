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
      <el-form-item label="企业名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入企业名称"
          clearable
          @keyup.enter="handleQuery"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item label="统一社会信用代码" prop="creditCode">
        <el-input
          v-model="queryParams.creditCode"
          placeholder="请输入统一社会信用代码"
          clearable
          @keyup.enter="handleQuery"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item label="企业类型" prop="enterpriseType">
        <el-select
          v-model="queryParams.enterpriseType"
          placeholder="请选择企业类型"
          clearable
          class="!w-240px"
        >
          <el-option
            v-for="dict in getIntDictOptions(DICT_TYPE.ENTERPRISE_TYPE_ENUM)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="成立日期" prop="establishmentDate">
        <el-date-picker
          v-model="queryParams.establishmentDate"
          value-format="YYYY-MM-DD HH:mm:ss"
          type="daterange"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          :default-time="[new Date('1 00:00:00'), new Date('1 23:59:59')]"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item label="联系人" prop="contactName">
        <el-input
          v-model="queryParams.contactName"
          placeholder="请输入联系人姓名"
          clearable
          @keyup.enter="handleQuery"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item label="联系电话" prop="contactPhone">
        <el-input
          v-model="queryParams.contactPhone"
          placeholder="请输入联系电话"
          clearable
          @keyup.enter="handleQuery"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item label="企业状态" prop="status">
        <el-select
          v-model="queryParams.status"
          placeholder="请选择企业状态"
          clearable
          class="!w-240px"
        >
          <el-option
            v-for="dict in getIntDictOptions(DICT_TYPE.ENTERPRISE_INFO_STATUS)"
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
          v-hasPermi="['enterprise:info:create']"
        >
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
        <el-button
          type="success"
          plain
          @click="handleExport"
          :loading="exportLoading"
          v-hasPermi="['enterprise:info:export']"
        >
          <Icon icon="ep:download" class="mr-5px" /> 导出
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 列表 -->
  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="ID" align="center" prop="id" width="80" />
      <el-table-column label="企业名称" align="center" prop="name" min-width="160" />
      <el-table-column label="统一社会信用代码" align="center" prop="creditCode" min-width="160" />
      <el-table-column label="企业类型" align="center" prop="enterpriseType" width="120">
        <template #default="scope">
          <dict-tag :type="DICT_TYPE.ENTERPRISE_TYPE_ENUM" :value="scope.row.enterpriseType || 0" />
        </template>
      </el-table-column>
      <el-table-column label="成立日期" align="center" prop="establishmentDate" :formatter="dateFormatter2" width="120" />
      <el-table-column label="联系人" align="center" prop="contactName" width="120" />
      <el-table-column label="联系电话" align="center" prop="contactPhone" width="120" />
      <el-table-column label="企业状态" align="center" prop="status" width="100">
        <template #default="scope">
          <dict-tag :type="DICT_TYPE.ENTERPRISE_INFO_STATUS" :value="scope.row.status || 0" />
        </template>
      </el-table-column> 
      <el-table-column
        label="创建时间"
        align="center"
        prop="createTime"
        :formatter="dateFormatter"
        width="160"
      />
      <el-table-column label="操作" align="center" fixed="right" width="180">
        <template #default="scope">
          <el-button
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
            v-hasPermi="['enterprise:info:update']"
          >
            编辑
          </el-button>
          <el-button
            link
            type="primary"
            @click="handleDetail(scope.row.id)"
            v-hasPermi="['enterprise:info:query']"
          >
            详情
          </el-button>
          <el-button
            link
            type="danger"
            @click="handleDelete(scope.row.id)"
            v-hasPermi="['enterprise:info:delete']"
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
  <InfoForm ref="formRef" @success="getList" />
</template>

<script setup lang="ts">
import { getIntDictOptions, DICT_TYPE } from '@/utils/dict'
import { dateFormatter, dateFormatter2 } from '@/utils/formatTime'
import download from '@/utils/download'
import { InfoApi, InfoVO } from '@/api/enterprise/info'
import InfoForm from './InfoForm.vue'

/** 企业信息 列表 */
defineOptions({ name: 'Info' })

const message = useMessage() // 消息弹窗
const { t } = useI18n() // 国际化

const loading = ref(true) // 列表的加载中
const list = ref<InfoVO[]>([]) // 列表的数据
const total = ref(0) // 列表的总页数
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  name: undefined,
  creditCode: undefined,
  enterpriseType: undefined,
  establishmentDate: [],
  registeredAddressProvinceCode: undefined,
  registeredAddressCityCode: undefined,
  registeredAddressDistrictCode: undefined,
  registeredAddressDetail: undefined,
  contactName: undefined,
  contactPhone: undefined,
  status: undefined,
  createTime: []
})
const queryFormRef = ref() // 搜索的表单
const exportLoading = ref(false) // 导出的加载中

/** 查询列表 */
const getList = async () => {
  loading.value = true
  try {
    const data = await InfoApi.getInfoPage(queryParams)
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
const formRef = ref()
const openForm = (type: string, id?: number) => {
  formRef.value.open(type, id)
}

/** 删除按钮操作 */
const handleDelete = async (id: number) => {
  try {
    // 删除的二次确认
    await message.delConfirm()
    // 发起删除
    await InfoApi.deleteInfo(id)
    message.success(t('common.delSuccess'))
    // 刷新列表
    await getList()
  } catch {}
}

/** 导出按钮操作 */
const handleExport = async () => {
  try {
    // 导出的二次确认
    await message.exportConfirm()
    // 发起导出
    exportLoading.value = true
    const data = await InfoApi.exportInfo(queryParams)
    download.excel(data, '企业信息.xls')
  } catch {
  } finally {
    exportLoading.value = false
  }
}

/** 查看详情操作 */
const handleDetail = (id: number) => {
  router.push({ path: `/enterprise/info/detail/${id}` })
}

/** 初始化 **/
const router = useRouter() // 用于页面跳转
onMounted(() => {
  getList()
})
</script>