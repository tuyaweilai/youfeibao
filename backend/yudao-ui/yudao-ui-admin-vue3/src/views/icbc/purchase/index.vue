<template>
  <ContentWrap title="采购管理">
    <el-alert
      type="info"
      :closable="false"
      class="mb-10px"
      title="采购管理承载采购合同与采购订单：向哪个交易对方买哪些品类、多少量、什么价格、在哪段时间内，以及每张订单的计划 / 验收 / 入库 / 结算 / 未履行五口径执行进度。"
    />
    <el-empty description="采购合同（T07）、采购订单（T08）与执行进度（T09）落地后，菜单会挂到这里。" />
  </ContentWrap>
</template>

<script setup lang="ts">
defineOptions({ name: 'IcbcPurchase' })
</script>
