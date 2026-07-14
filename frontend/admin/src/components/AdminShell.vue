<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'

import chevronDownIcon from '@fluentui/svg-icons/icons/chevron_down_20_regular.svg?url'
import chevronLeftIcon from '@fluentui/svg-icons/icons/chevron_left_24_regular.svg?url'
import chevronRightIcon from '@fluentui/svg-icons/icons/chevron_right_24_regular.svg?url'
import commentIcon from '@fluentui/svg-icons/icons/comment_24_regular.svg?url'
import documentIcon from '@fluentui/svg-icons/icons/document_24_regular.svg?url'
import folderIcon from '@fluentui/svg-icons/icons/folder_24_regular.svg?url'
import gridIcon from '@fluentui/svg-icons/icons/grid_24_regular.svg?url'
import historyIcon from '@fluentui/svg-icons/icons/history_24_regular.svg?url'
import homeIcon from '@fluentui/svg-icons/icons/home_24_regular.svg?url'
import imageIcon from '@fluentui/svg-icons/icons/image_24_regular.svg?url'
import personIcon from '@fluentui/svg-icons/icons/person_24_regular.svg?url'
import signOutIcon from '@fluentui/svg-icons/icons/sign_out_24_regular.svg?url'
import tagIcon from '@fluentui/svg-icons/icons/tag_24_regular.svg?url'

import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const collapsed = ref(localStorage.getItem('admin-sidebar-collapsed') === 'true')
const taxonomyOpen = ref(['categories', 'tags'].includes(String(route.name ?? '')))

const navigation = [
  { label: '仪表盘', to: '/', icon: homeIcon, match: ['dashboard'] },
  { label: '文章管理', to: '/articles', icon: documentIcon, match: ['articles', 'article-new', 'article-edit'] },
  { label: '媒体管理', to: '/media', icon: imageIcon, match: ['media'] },
  { label: '评论管理', to: '/comments', icon: commentIcon, match: ['comments'] },
  { label: '操作日志', to: '/operation-logs', icon: historyIcon, match: ['operation-logs'] }
]

const activeName = computed(() => String(route.name ?? ''))
const taxonomyActive = computed(() => ['categories', 'tags'].includes(activeName.value))
const pageTitle = computed(() => String(route.meta.title ?? '管理'))
const displayName = computed(() => auth.user?.nickname || auth.user?.username || '管理员')
const isEditorPage = computed(() => ['article-new', 'article-edit'].includes(activeName.value))

watch(taxonomyActive, (active) => {
  if (active) taxonomyOpen.value = true
})

const toggleSidebar = () => {
  collapsed.value = !collapsed.value
  localStorage.setItem('admin-sidebar-collapsed', String(collapsed.value))
}

const logout = async () => {
  await auth.logout()
  await router.replace('/login')
}
</script>

<template>
  <div class="admin-shell" :class="{ 'sidebar-collapsed': collapsed }">
    <aside class="admin-sidebar" :class="{ 'is-collapsed': collapsed }">
      <RouterLink class="sidebar-brand" to="/" aria-label="返回管理总览">
        <span class="sidebar-brand-mark"><img :src="gridIcon" alt=""></span>
        <strong v-if="!collapsed">Wineclouds’Blog</strong>
      </RouterLink>

      <nav class="sidebar-nav" aria-label="管理后台主导航">
        <RouterLink
          v-for="item in navigation.slice(0, 2)"
          :key="item.to"
          :to="item.to"
          class="sidebar-nav-item"
          :class="{ active: item.match.includes(activeName) }"
          :title="collapsed ? item.label : undefined"
        >
          <img :src="item.icon" alt="">
          <span v-if="!collapsed">{{ item.label }}</span>
        </RouterLink>

        <div class="taxonomy-group" :class="{ 'is-open': taxonomyOpen, active: taxonomyActive }">
          <button
            class="sidebar-nav-item taxonomy-trigger"
            type="button"
            :aria-expanded="taxonomyOpen"
            title="分类 / 标签"
            @click="taxonomyOpen = !taxonomyOpen"
          >
            <img :src="folderIcon" alt="">
            <span v-if="!collapsed">分类 / 标签</span>
            <img v-if="!collapsed" class="taxonomy-chevron" :src="chevronDownIcon" alt="">
          </button>
          <div v-if="taxonomyOpen" class="taxonomy-children">
            <RouterLink to="/categories" :class="{ active: activeName === 'categories' }">
              <img :src="folderIcon" alt="">
              <span>分类管理</span>
            </RouterLink>
            <RouterLink to="/tags" :class="{ active: activeName === 'tags' }">
              <img :src="tagIcon" alt="">
              <span>标签管理</span>
            </RouterLink>
          </div>
        </div>

        <RouterLink
          v-for="item in navigation.slice(2)"
          :key="item.to"
          :to="item.to"
          class="sidebar-nav-item"
          :class="{ active: item.match.includes(activeName) }"
          :title="collapsed ? item.label : undefined"
        >
          <img :src="item.icon" alt="">
          <span v-if="!collapsed">{{ item.label }}</span>
        </RouterLink>
      </nav>

      <button class="sidebar-collapse" type="button" :aria-label="collapsed ? '展开侧栏' : '折叠侧栏'" @click="toggleSidebar">
        <img :src="collapsed ? chevronRightIcon : chevronLeftIcon" alt="">
        <span v-if="!collapsed">折叠侧栏</span>
      </button>
    </aside>

    <section class="admin-main-wrapper">
      <header class="admin-topbar">
        <nav class="admin-breadcrumb" aria-label="面包屑">
          <RouterLink to="/">首页</RouterLink>
          <span>/</span>
          <strong>{{ pageTitle }}</strong>
        </nav>
        <div class="admin-account">
          <span class="admin-user"><img :src="personIcon" alt="">{{ displayName }}</span>
          <button type="button" @click="logout"><img :src="signOutIcon" alt="">退出</button>
        </div>
      </header>

      <main class="admin-page-main" :class="{ 'editor-main': isEditorPage }">
        <slot />
      </main>
    </section>
  </div>
</template>
