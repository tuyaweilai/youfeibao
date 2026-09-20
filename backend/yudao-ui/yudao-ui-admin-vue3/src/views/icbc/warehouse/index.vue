<template>
  <ContentWrap title="仓储管理">
    <el-alert
      type="info"
      :closable="false"
      class="mb-10px"
      title="仓储管理以 yudao ERP 的库存域为底座：待入库、入库单、出入库流水、库存查询与盘点调整；库位与批次是库存的维度，收购单是唯一事实源、入库是它的派生动作。"
    />
    <el-empty description="待入库与入库单（T14）、库位与批次（T05）、非销售出库 / 调拨 / 盘点（T16）落地后，菜单会挂到这里。" />
  </ContentWrap>
</template>

<script setup lang="ts">
defineOptions({ name: 'IcbcWarehouse' })
</script>
