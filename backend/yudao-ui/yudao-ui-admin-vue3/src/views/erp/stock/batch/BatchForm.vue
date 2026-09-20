<!-- ERP 批次表单 -->
<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="100px"
      v-loading="formLoading"
    >
      <el-form-item label="批次号" prop="batchNo">
        <el-input v-model="formData.batchNo" placeholder="如 B20260920-01" />
      </el-form-item>
      <el-form-item label="品类" prop="goodsConfigId">
        <el-select
          v-model="formData.goodsConfigId"
          clearable
          filterable
          placeholder="不限定品类可留空"
          class="!w-1/1"
        >
          <el-option
            v-for="item in goodsConfigList"
            :key="item.id"
            :label="item.name"
            :value="item.id!"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="入库时间" prop="inTime">
        <el-date-picker
          v-model="formData.inTime"
          type="datetime"
          value-format="x"
          placeholder="请选择入库时间"
          class="!w-1/1"
        />
      </el-form-item>
      <el-form-item label="开启状态" prop="status">
        <el-radio-group v-model="formData.status">
          <el-radio
            v-for="dict in getIntDictOptions(DICT_TYPE.COMMON_STATUS)"
            :key="dict.value"
            :value="dict.value"
          >
            {{ dict.label }}
          </el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input type="textarea" v-model="formData.remark" placeholder="请输入备注" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="submitForm" type="primary" :disabled="formLoading">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>
<script setup lang="ts">
import { getIntDictOptions, DICT_TYPE } from '@/utils/dict'
import { StockBatchApi, StockBatchVO } from '@/api/erp/stock/batch'
import { GoodsConfigApi, GoodsConfigVO } from '@/api/icbc/goodsConfig'
import { CommonStatusEnum } from '@/utils/constants'

/** ERP 批次表单 */
defineOptions({ name: 'StockBatchForm' })

const { t } = useI18n() // 国际化
const message = useMessage() // 消息弹窗

const dialogVisible = ref(false) // 弹窗的是否展示
const dialogTitle = ref('') // 弹窗的标题
const formLoading = ref(false) // 表单的加载中
const formType = ref('') // 表单的类型：create - 新增；update - 修改
const goodsConfigList = ref<GoodsConfigVO[]>([]) // 品类列表
const formData = ref<StockBatchVO>({
  id: undefined,
  batchNo: undefined,
  goodsConfigId: undefined,
  inTime: undefined,
  remark: undefined,
  status: CommonStatusEnum.ENABLE
})
const formRules = reactive({
  batchNo: [{ required: true, message: '批次号不能为空', trigger: 'blur' }],
  status: [{ required: true, message: '开启状态不能为空', trigger: 'change' }]
})
const formRef = ref() // 表单 Ref

/** 打开弹窗 */
const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  // 加载品类下拉
  goodsConfigList.value = await GoodsConfigApi.getEnabledList()
  // 修改时，设置数据
  if (id) {
    formLoading.value = true
    try {
      formData.value = await StockBatchApi.getStockBatch(id)
    } finally {
      formLoading.value = false
    }
  }
}
defineExpose({ open }) // 提供 open 方法，用于打开弹窗

/** 提交表单 */
const emit = defineEmits(['success']) // 定义 success 事件，用于操作成功后的回调
const submitForm = async () => {
  await formRef.value.validate()
  formLoading.value = true
  try {
    const data = formData.value
    if (formType.value === 'create') {
      await StockBatchApi.createStockBatch(data)
      message.success(t('common.createSuccess'))
    } else {
      await StockBatchApi.updateStockBatch(data)
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
    id: undefined,
    batchNo: undefined,
    goodsConfigId: undefined,
    inTime: undefined,
    remark: undefined,
    status: CommonStatusEnum.ENABLE
  }
  formRef.value?.resetFields()
}
</script>
