<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

import AdminShell from './components/AdminShell.vue'

const route = useRoute()
const usesAdminShell = computed(() => Boolean(route.meta.requiresAuth))
</script>

<template>
  <AdminShell v-if="usesAdminShell">
    <RouterView v-slot="{ Component }">
      <Transition name="admin-page-fade" mode="out-in">
        <component :is="Component" :key="route.fullPath" />
      </Transition>
    </RouterView>
  </AdminShell>
  <RouterView v-else v-slot="{ Component }">
    <Transition name="admin-page-fade" mode="out-in">
      <component :is="Component" :key="route.fullPath" />
    </Transition>
  </RouterView>
</template>
