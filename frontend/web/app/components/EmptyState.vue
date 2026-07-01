<script setup lang="ts">
defineProps<{ title?: string; description?: string }>()
</script>

<template>
  <div class="empty-state">
    <span aria-hidden="true">○</span>
    <h2>{{ title || '这里暂时还是一片余白' }}</h2>
    <p>{{ description || '内容正在路上，过一阵再来看看吧。' }}</p>
  </div>
</template>
