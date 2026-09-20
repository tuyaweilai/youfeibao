<template>
  <ContentWrap title="工作台">
    <el-alert
      type="info"
      :closable="false"
      class="mb-10px"
      title="工作台将汇总今天该处理的事与预警：今日到场、待称重、待验收、待入库、待结算确认、异议、付款/票务失败、额度与资质预警，以及开票就绪状态徽标。"
    />
    <el-empty description="待办与预警由 T18 落地；此处先占位，保证菜单骨架可点。" />
  </ContentWrap>
</template>

<script setup lang="ts">
defineOptions({ name: 'IcbcWorkbench' })
</script>
