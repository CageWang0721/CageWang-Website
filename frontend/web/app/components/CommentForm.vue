<script setup lang="ts">
import type { CommentInput } from '~/types/blog'

const props = withDefaults(defineProps<{
  articleId: number
  parentId?: number | null
  compact?: boolean
}>(), {
  parentId: null,
  compact: false
})

const emit = defineEmits<{ submitted: [] }>()
const api = useBlogApi()
const form = reactive<CommentInput>({
  nickname: '',
  email: '',
  website: '',
  content: '',
  parentId: props.parentId,
  notifyOnReply: false
})
const submitting = ref(false)
const error = ref('')
const success = ref('')

const submit = async () => {
  if (submitting.value) return
  submitting.value = true
  error.value = ''
  success.value = ''
  try {
    const result = await api.submitComment(props.articleId, form)
    success.value = result.message
    form.content = ''
    emit('submitted')
  } catch (cause: any) {
    error.value = cause?.data?.detail || cause?.message || '提交失败，请稍后重试'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <form class="comment-form" :class="{ compact }" @submit.prevent="submit">
    <div class="comment-fields">
      <label>
        <span>昵称 *</span>
        <input v-model.trim="form.nickname" required minlength="2" maxlength="64" autocomplete="name">
      </label>
      <label>
        <span>邮箱</span>
        <input v-model.trim="form.email" type="email" maxlength="160" autocomplete="email">
      </label>
      <label v-if="!compact">
        <span>个人网站</span>
        <input v-model.trim="form.website" type="url" maxlength="300" placeholder="https://">
      </label>
    </div>
    <label class="comment-content">
      <span>{{ compact ? '写下回复 *' : '写下想说的话 *' }}</span>
      <textarea
        v-model.trim="form.content"
        required
        minlength="2"
        maxlength="2000"
        :rows="compact ? 4 : 6"
        placeholder="支持 Markdown；审核通过后公开显示。"
      />
    </label>
    <div class="comment-submit">
      <button class="button" type="submit" :disabled="submitting">
        {{ submitting ? '正在投递…' : compact ? '提交回复' : '提交' }}
      </button>
    </div>
    <p v-if="error" class="form-message error">{{ error }}</p>
    <p v-if="success" class="form-message success">{{ success }}</p>
  </form>
</template>
