<template>
  <el-alert v-if="definition" type="info" :closable="false" class="mb-10px" :title="'口径：' + definition" />
</template>

<script setup lang="ts">
defineProps<{
  definition?: string
}>()
</script>
