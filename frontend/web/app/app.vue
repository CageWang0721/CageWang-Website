<script setup lang="ts">
useSiteStatisticsPolling()

const glassReady = ref(false)

const wait = (duration: number) => new Promise<void>((resolve) => {
  window.setTimeout(resolve, duration)
})

const waitForPaint = () => new Promise<void>((resolve) => {
  window.requestAnimationFrame(() => resolve())
})

const decodeWallpaper = async () => {
  const wallpaper = new Image()
  wallpaper.decoding = 'async'
  wallpaper.src = '/images/railen-wallpaper.webp'

  try {
    await wallpaper.decode()
  } catch {
    // Keep the page usable when the wallpaper cannot be decoded.
  }
}

onMounted(async () => {
  await Promise.all([
    wait(180),
    Promise.race([decodeWallpaper(), wait(900)])
  ])
  await waitForPaint()
  await waitForPaint()
  glassReady.value = true
})
</script>

<template>
  <div
    class="site-shell"
    :class="{
      'glass-render-ready': glassReady
    }"
  >
    <SnowfallOverlay />
    <SiteHeader />

    <main>
      <NuxtPage :transition="{ name: 'page', mode: 'out-in' }" />
    </main>

    <SiteFooter />
  </div>
</template>
