<template>
  <ContentWrap>
    <el-alert
      type="info"
      :closable="false"
      class="mb-10px"
      title="平台级自然人主体：以身份证件号码为唯一锚点，跨回收企业复用。一个人只有一个主体与一个工行外部用户编号，实人认证一次即可。"
    />
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="120px">
      <el-form-item label="姓名" prop="name">
        <el-input v-model="queryParams.name" placeholder="请输入" clearable @keyup.enter="handleQuery" class="!w-200px" />
      </el-form-item>
      <el-form-item label="身份证件号码" prop="idCardNo">
        <el-input v-model="queryParams.idCardNo" placeholder="请输入完整号码" clearable @keyup.enter="handleQuery" class="!w-220px" />
      </el-form-item>
      <el-form-item label="手机号" prop="mobile">
        <el-input v-model="queryParams.mobile" placeholder="请输入" clearable @keyup.enter="handleQuery" class="!w-200px" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择" clearable class="!w-140px">
          <el-option label="正常" :value="0" />
          <el-option label="已停用" :value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true">
      <el-table-column label="ID" align="center" prop="id" width="70" />
      <el-table-column label="姓名" prop="name" min-width="110" />
      <el-table-column label="身份证件号码" prop="idCardNo" min-width="180" />
      <el-table-column label="手机号" prop="mobile" width="140" />
      <el-table-column label="外部用户编号" prop="outUserId" min-width="200" />
      <el-table-column label="实人认证" align="center" prop="realNameStatusName" width="110" />
      <el-table-column label="已绑定登录凭证" align="center" width="150">
        <template #default="{ row }">
          <span v-if="!row.memberUserIds || row.memberUserIds.length === 0" class="text-gray-400">未绑定</span>
          <el-tag v-for="id in row.memberUserIds" :key="id" class="mr-5px">{{ id }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 0 ? 'success' : 'danger'">{{ row.status === 0 ? '正常' : '已停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="230" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openClaim(row)" v-hasPermi="['icbc:platform:natural-person:manage']">
            身份认领
          </el-button>
          <el-button link type="primary" @click="openUnbind(row)" v-hasPermi="['icbc:platform:natural-person:manage']">
            解绑
          </el-button>
          <el-button link type="danger" @click="toggleStatus(row)" v-hasPermi="['icbc:platform:natural-person:manage']">
            {{ row.status === 0 ? '停用' : '恢复' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </ContentWrap>

  <el-dialog v-model="dialogVisible" :title="dialogTitle" width="480px">
    <el-alert
      type="warning"
      :closable="false"
      class="mb-10px"
      title="这里是人工入口：自动化流程遇到「同一身份证已建档、手机号不一致」时只会拒绝，认领与裁决由人工核实后完成。请在备注里写清核实过程。"
    />
    <el-form :model="form" label-width="130px">
      <el-form-item label="自然人主体">
        <span>{{ current?.name }}（ID {{ current?.id }}）</span>
      </el-form-item>
      <el-form-item label="登录凭证编号" v-if="dialogType === 'claim'">
        <el-input v-model="form.memberUserId" placeholder="会员用户编号（member_user.id）" />
      </el-form-item>
      <el-form-item label="登录凭证编号" v-else>
        <el-select v-model="form.memberUserId" placeholder="选择要解绑的凭证" class="!w-100%">
          <el-option v-for="id in current?.memberUserIds || []" :key="id" :label="String(id)" :value="id" />
        </el-select>
      </el-form-item>
      <el-form-item label="核实过程" v-if="dialogType === 'claim'">
        <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="例如：电话核实，本人报出身份证后四位与开户行" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleSubmit">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { NaturalPersonApi, NaturalPersonVO } from '@/api/icbc/naturalPerson'

defineOptions({ name: 'IcbcNaturalPerson' })

const message = useMessage()

const loading = ref(true)
const list = ref<NaturalPersonVO[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  name: undefined,
  idCardNo: undefined,
  mobile: undefined,
  status: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await NaturalPersonApi.getNaturalPersonPage(queryParams)
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

const dialogVisible = ref(false)
const dialogType = ref<'claim' | 'unbind'>('claim')
const dialogTitle = computed(() => (dialogType.value === 'claim' ? '身份认领' : '解绑登录凭证'))
const current = ref<NaturalPersonVO>()
const form = reactive<{ memberUserId?: number; remark?: string }>({})

const openClaim = (row: NaturalPersonVO) => {
  current.value = row
  dialogType.value = 'claim'
  form.memberUserId = undefined
  form.remark = undefined
  dialogVisible.value = true
}
const openUnbind = (row: NaturalPersonVO) => {
  current.value = row
  dialogType.value = 'unbind'
  form.memberUserId = row.memberUserIds && row.memberUserIds.length > 0 ? row.memberUserIds[0] : undefined
  dialogVisible.value = true
}
const handleSubmit = async () => {
  if (!current.value?.id || !form.memberUserId) {
    message.warning('请填写登录凭证编号')
    return
  }
  if (dialogType.value === 'claim') {
    await NaturalPersonApi.claim({
      naturalPersonId: current.value.id,
      memberUserId: form.memberUserId,
      remark: form.remark
    })
    message.success('已绑定该登录凭证')
  } else {
    await NaturalPersonApi.unbind({
      naturalPersonId: current.value.id,
      memberUserId: form.memberUserId
    })
    message.success('已解绑（主体与交易记录保留）')
  }
  dialogVisible.value = false
  await getList()
}
const toggleStatus = async (row: NaturalPersonVO) => {
  const next = row.status === 0 ? 1 : 0
  const { value } = await message.prompt(
    next === 1 ? '停用原因（如冒用核实）' : '恢复原因',
    next === 1 ? '停用身份' : '恢复身份'
  )
  await NaturalPersonApi.updateStatus(row.id!, next, value)
  message.success(next === 1 ? '已停用' : '已恢复')
  await getList()
}

onMounted(getList)
</script>
