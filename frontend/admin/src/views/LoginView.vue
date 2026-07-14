<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ApiError } from '@personal-blog/api-client'
import gridIcon from '@fluentui/svg-icons/icons/grid_24_regular.svg?url'
import lockIcon from '@fluentui/svg-icons/icons/lock_closed_24_regular.svg?url'
import personIcon from '@fluentui/svg-icons/icons/person_24_regular.svg?url'

import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const username = ref('')
const password = ref('')
const submitting = ref(false)
const error = ref('')

const submit = async () => {
  if (!username.value || !password.value || submitting.value) return
  submitting.value = true
  error.value = ''
  try {
    await auth.login(username.value, password.value)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    await router.replace(redirect)
  } catch (cause) {
    error.value = cause instanceof ApiError ? cause.message : '登录服务暂时不可用'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <main class="login-page">
    <section class="login-card">
      <div class="login-card-header">
        <span class="login-brand-icon"><img :src="gridIcon" alt=""></span>
        <h1>管理控制台</h1>
        <p>Wineclouds’Blog</p>
      </div>
      <form class="login-form" @submit.prevent="submit">
        <label class="login-field">
          <span class="visually-hidden">用户名</span>
          <img :src="personIcon" alt="">
          <input v-model.trim="username" name="username" autocomplete="username" maxlength="64" placeholder="用户名" autofocus>
        </label>
        <label class="login-field">
          <span class="visually-hidden">密码</span>
          <img :src="lockIcon" alt="">
          <input v-model="password" name="password" type="password" autocomplete="current-password" maxlength="200" placeholder="登录密码">
        </label>
        <p v-if="error" class="login-error" role="alert">{{ error }}</p>
        <button type="submit" :disabled="submitting || !username || !password">
          {{ submitting ? '正在验证…' : '登 录' }}
        </button>
      </form>
    </section>
  </main>
</template>
