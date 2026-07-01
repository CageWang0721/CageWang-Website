<script setup lang="ts">
defineProps<{
  eyebrow: string
  title: string
  description?: string
  count?: number
}>()
</script>

<template>
  <header class="page-intro content-width">
    <p class="eyebrow">{{ eyebrow }}</p>
    <div class="intro-title-row">
      <h1>{{ title }}</h1>
      <span v-if="count !== undefined" class="count-badge">{{ count }}</span>
    </div>
    <p v-if="description" class="intro-description">{{ description }}</p>
  </header>
</template>
