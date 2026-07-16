<script setup lang="ts">
const snowflakes = Array.from({ length: 28 }, (_, index) => ({
  delay: `-${(index * 3) % 22}s`,
  drift: `${-18 + (index % 7) * 6}px`,
  duration: `${14 + (index % 6) * 2}s`,
  left: `${(index * 37 + 7) % 100}%`,
  opacity: `${0.28 + (index % 4) * 0.1}`,
  size: `${3 + (index % 4)}px`
}))
</script>

<template>
  <div class="snowfall-overlay" aria-hidden="true">
    <span
      v-for="(snowflake, index) in snowflakes"
      :key="index"
      class="snowfall-overlay__flake"
      :style="{
        '--snowflake-delay': snowflake.delay,
        '--snowflake-drift': snowflake.drift,
        '--snowflake-duration': snowflake.duration,
        '--snowflake-left': snowflake.left,
        '--snowflake-opacity': snowflake.opacity,
        '--snowflake-size': snowflake.size
      }"
    />
  </div>
</template>
