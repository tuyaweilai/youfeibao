<template>
  <ContentWrap>
    <el-alert type="info" :closable="false" class="mb-10px" :title="form.scopeNote" />
    <el-form :model="form" label-width="200px" v-loading="loading">
      <el-form-item label="完成比例采用的履约口径">
        <el-select v-model="form.performanceBasis" class="!w-300px">
          <el-option
            v-for="item in PURCHASE_PERFORMANCE_BASIS_OPTIONS"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <div class="text-gray-500 text-12px">{{ form.performanceBasisDefinition }}</div>
      </el-form-item>
      <el-form-item label="超量交货">
        <el-radio-group v-model="form.overQuantityRule">
          <el-radio v-for="item in PURCHASE_DELIVERY_RULE_OPTIONS" :key="item.value" :label="item.value">
            {{ item.label }}
          </el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="过期交货">
        <el-radio-group v-model="form.expiredRule">
          <el-radio v-for="item in PURCHASE_DELIVERY_RULE_OPTIONS" :key="item.value" :label="item.value">
            {{ item.label }}
          </el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="跨场站交货">
        <el-radio-group v-model="form.crossStationRule">
          <el-radio v-for="item in PURCHASE_DELIVERY_RULE_OPTIONS" :key="item.value" :label="item.value">
            {{ item.label }}
          </el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="form.remark" type="textarea" :rows="2" class="!w-500px" />
      </el-form-item>
      <el-form-item>
        <el-button
          type="primary"
          @click="submit"
          v-hasPermi="['icbc:purchase-setting:manage']"
        >
          保 存
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import {
  PurchaseOrderApi,
  PurchaseOrderSettingVO,
  PURCHASE_PERFORMANCE_BASIS_OPTIONS,
  PURCHASE_DELIVERY_RULE_OPTIONS
} from '@/api/icbc/purchaseOrder'

defineOptions({ name: 'IcbcPurchaseOrderSetting' })

const message = useMessage()
const loading = ref(true)
const form = reactive<PurchaseOrderSettingVO>({})

const getSetting = async () => {
  loading.value = true
  try {
    Object.assign(form, await PurchaseOrderApi.getSetting())
  } finally {
    loading.value = false
  }
}

const submit = async () => {
  if (!form.performanceBasis || !form.overQuantityRule || !form.expiredRule || !form.crossStationRule) {
    message.warning('请把四项都选完')
    return
  }
  await PurchaseOrderApi.updateSetting(form)
  message.success('已保存（对之后登记的交货生效）')
  await getSetting()
}

getSetting()
</script>
