<template>
  <ContentWrap title="经营报表">
    <el-alert
      type="info"
      :closable="false"
      class="mb-10px"
      title="经营报表分采购履约 / 收购台账 / 库存 / 结算付款四张，每张标注采用哪个口径，不混用计划、验收、入库、结算与实物数量。"
    />
    <el-empty description="经营报表与异常表由 T19 落地；此处先占位，保证菜单骨架可点。" />
  </ContentWrap>
</template>

<script setup lang="ts">
defineOptions({ name: 'IcbcReport' })
</script>
