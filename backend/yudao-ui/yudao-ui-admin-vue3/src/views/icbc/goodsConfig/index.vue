<template>
  <ContentWrap>
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="90px">
      <el-form-item label="品类名称" prop="name">
        <el-input v-model="queryParams.name" placeholder="请输入" clearable @keyup.enter="handleQuery" class="!w-220px" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择" clearable class="!w-160px">
          <el-option label="启用" :value="0" />
          <el-option label="停用" :value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" plain @click="openForm('create')" v-hasPermi="['icbc:goods-config:create']">
          <Icon icon="ep:plus" class="mr-5px" /> 新增品类
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true">
      <el-table-column label="ID" align="center" prop="id" width="70" />
      <el-table-column label="品类名称" prop="name" min-width="160" />
      <el-table-column label="计量单位" align="center" prop="unit" width="110" />
      <el-table-column label="税率" align="center" prop="taxRate" width="100" />
      <el-table-column label="计税方法" align="center" prop="taxMethod" width="110">
        <template #default="{ row }">
          <el-tag :type="row.taxMethod === 'SIMPLE' ? 'warning' : 'info'">
            {{ row.taxMethod === 'SIMPLE' ? '简易计税' : '一般计税' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="税收分类合并编码" prop="mergedCode" min-width="220" />
      <el-table-column label="状态" align="center" prop="status" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 0 ? 'success' : 'info'">{{ row.status === 0 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="备注" prop="remark" min-width="160" />
      <el-table-column label="操作" align="center" width="140" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openForm('update', row.id)" v-hasPermi="['icbc:goods-config:update']">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row.id)" v-hasPermi="['icbc:goods-config:delete']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </ContentWrap>

  <GoodsConfigForm ref="formRef" @success="getList" />
</template>

<script setup lang="ts">
import { GoodsConfigApi, GoodsConfigVO } from '@/api/icbc/goodsConfig'
import GoodsConfigForm from './GoodsConfigForm.vue'

defineOptions({ name: 'IcbcGoodsConfig' })

const message = useMessage()
const { t } = useI18n()

const loading = ref(true)
const list = ref<GoodsConfigVO[]>([])
const total = ref(0)
const queryParams = reactive({ pageNo: 1, pageSize: 10, name: undefined, status: undefined })
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await GoodsConfigApi.getGoodsConfigPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}
const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}
const resetQuery = () => {
  queryFormRef.value.resetFields()
  handleQuery()
}
const formRef = ref()
const openForm = (type: string, id?: number) => formRef.value.open(type, id)
const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await GoodsConfigApi.deleteGoodsConfig(id)
    message.success(t('common.delSuccess'))
    await getList()
  } catch {}
}

onMounted(getList)
</script>
