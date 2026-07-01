<script setup lang="ts">
defineProps<{ error: { statusCode: number; statusMessage?: string } }>()
</script>

<template>
  <div class="error-page">
    <p>{{ error.statusCode }}</p>
    <h1>{{ error.statusCode === 404 ? '这一页被风吹走了' : '页面暂时走神了' }}</h1>
    <span>{{ error.statusMessage || '不妨回到首页，从另一条路继续。' }}</span>
    <NuxtLink class="button primary" to="/">回到首页</NuxtLink>
  </div>
</template>
