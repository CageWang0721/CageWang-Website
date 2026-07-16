<script setup lang="ts">
useSiteStatisticsPolling()
</script>

<template>
  <div class="site-shell">
    <SnowfallOverlay />
    <SiteHeader />

    <main>
      <NuxtPage />
    </main>

    <SiteFooter />
  </div>
</template>
