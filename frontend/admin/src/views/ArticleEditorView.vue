<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'

import { ApiError } from '@personal-blog/api-client'

import { api } from '../services/api'
import { useAuthStore } from '../stores/auth'
import WordLikeEditor from '../components/WordLikeEditor.vue'
import type {
  ArticleDetail,
  ArticleInput,
  ArticleStatus,
  TaxonomyOption
} from '../types/article'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const id = computed(() => route.params.id ? Number(route.params.id) : null)
const isNew = computed(() => id.value === null)
const canWrite = computed(() => auth.user?.role === 'ADMIN')
const saving = ref(false)
const publishing = ref(false)
const withdrawing = ref(false)
const loading = ref(!isNew.value)
const error = ref('')
const success = ref('')
const savedAt = ref('')
const saveState = ref<'idle' | 'dirty' | 'saving' | 'saved' | 'error'>('idle')
const articleStatus = ref<ArticleStatus | null>(null)
const publishAt = ref('')
const publishConfirmationOpen = ref(false)
const previewHtml = ref('')
const previewStats = reactive({ wordCount: 0, readingMinutes: 0 })
const previewing = ref(false)
const ossConfigured = ref(false)
const maxImageSize = ref(10 * 1024 * 1024)
const ready = ref(false)
let saveTimer: ReturnType<typeof setTimeout> | undefined
let previewTimer: ReturnType<typeof setTimeout> | undefined
let savedFingerprint = ''
const categories = ref<TaxonomyOption[]>([])
const tags = ref<TaxonomyOption[]>([])
const editingScheduledArticle = computed(() => articleStatus.value === 'SCHEDULED')
const editingPublishedArticle = computed(() => (
  articleStatus.value === 'PUBLISHED' || articleStatus.value === 'SCHEDULED'
))
const editorBusy = computed(() => saving.value || publishing.value || withdrawing.value)
const editorLocked = computed(() => (
  !canWrite.value
  || publishing.value
  || withdrawing.value
  || (saving.value && editingPublishedArticle.value)
))

const form = reactive<ArticleInput>({
  title: '',
  slug: '',
  summary: '',
  contentMarkdown: '',
  categoryId: null,
  tagIds: [],
  visibility: 'PUBLIC',
  pinned: false,
  allowComment: true,
  metaTitle: '',
  metaDescription: '',
  canonicalUrl: '',
  version: 0
})
const selectedCategoryName = computed(() => (
  categories.value.find((category) => category.id === Number(form.categoryId))?.name || '未分类'
))
const visibilityName = computed(() => form.visibility === 'PUBLIC' ? '公开' : '私密')
const publishTimeName = computed(() => (
  publishAt.value ? new Date(publishAt.value).toLocaleString() : '立即发布'
))

const parseApiDate = (value: string) => new Date(
  /(?:Z|[+-]\d{2}:\d{2})$/.test(value) ? value : `${value}Z`
)

const toLocalDateTimeInput = (value: string) => {
  const date = parseApiDate(value)
  if (Number.isNaN(date.getTime())) return ''
  const pad = (part: number) => String(part).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
    + `T${pad(date.getHours())}:${pad(date.getMinutes())}`
}

const formatApiDate = (value?: string) => (
  value ? parseApiDate(value).toLocaleString() : publishTimeName.value
)

const applyArticle = (article: ArticleDetail) => {
  articleStatus.value = article.status
  if (article.publishedAt) publishAt.value = toLocalDateTimeInput(article.publishedAt)
  Object.assign(form, {
    title: article.title,
    slug: article.slug,
    summary: article.summary ?? '',
    contentMarkdown: article.contentMarkdown,
    categoryId: article.categoryId ?? null,
    tagIds: article.tagIds,
    visibility: article.visibility,
    pinned: article.pinned,
    allowComment: article.allowComment,
    metaTitle: article.metaTitle ?? '',
    metaDescription: article.metaDescription ?? '',
    canonicalUrl: article.canonicalUrl ?? '',
    version: article.version
  })
  savedFingerprint = JSON.stringify(form)
}

const updatePreview = async () => {
  previewing.value = true
  try {
    const result = await api.post<{
      html: string
      wordCount: number
      readingMinutes: number
    }>('/admin/markdown/preview', { markdown: form.contentMarkdown })
    previewHtml.value = result.html
    previewStats.wordCount = result.wordCount
    previewStats.readingMinutes = result.readingMinutes
  } catch {
    // Preview failures should not destroy the draft or interrupt editing.
  } finally {
    previewing.value = false
  }
}

const schedulePreview = () => {
  clearTimeout(previewTimer)
  previewTimer = setTimeout(() => void updatePreview(), 350)
}

const load = async () => {
  error.value = ''
  try {
    const [categoryOptions, tagOptions, mediaConfig] = await Promise.all([
      api.get<TaxonomyOption[]>('/admin/categories/options'),
      api.get<TaxonomyOption[]>('/admin/tags/options'),
      api.get<{ configured: boolean, maxImageSize: number }>('/admin/media/config')
    ])
    categories.value = categoryOptions
    tags.value = tagOptions
    ossConfigured.value = mediaConfig.configured
    maxImageSize.value = mediaConfig.maxImageSize
    if (id.value !== null) {
      applyArticle(await api.get<ArticleDetail>(`/admin/articles/${id.value}`))
    }
    await updatePreview()
  } catch (cause) {
    error.value = cause instanceof ApiError ? cause.message : '文章加载失败'
  } finally {
    loading.value = false
    ready.value = true
  }
}

const save = async (automatic = false) => {
  if (!canWrite.value) return null
  if (automatic && editingPublishedArticle.value) return null
  if (
    !automatic
    && editingPublishedArticle.value
    && saveState.value !== 'dirty'
    && saveState.value !== 'error'
  ) return null
  if (saving.value) {
    if (automatic) {
      clearTimeout(saveTimer)
      saveTimer = setTimeout(() => void save(true), 1000)
    }
    return null
  }
  error.value = ''
  if (!automatic) success.value = ''
  const missingFields = [
    ...(!form.title.trim() ? ['标题'] : []),
    ...(!form.slug.trim() ? ['slug'] : [])
  ]
  if (missingFields.length > 0) {
    if (!automatic) error.value = `${missingFields.join('和')}不能为空`
    return null
  }
  if (
    editingPublishedArticle.value
    && !window.confirm(editingScheduledArticle.value
      ? '这会更新等待定时发布的文章内容，确认继续吗？'
      : '这会立即更新公开站上的文章内容，确认继续吗？')
  ) return null
  saving.value = true
  saveState.value = 'saving'
  try {
    const wasNew = isNew.value
    const payload: ArticleInput = {
      title: form.title,
      slug: form.slug,
      summary: form.summary,
      contentMarkdown: form.contentMarkdown,
      categoryId: form.categoryId === null ? null : Number(form.categoryId),
      tagIds: form.tagIds.map(Number),
      visibility: form.visibility,
      pinned: Boolean(form.pinned),
      allowComment: Boolean(form.allowComment),
      metaTitle: form.metaTitle,
      metaDescription: form.metaDescription,
      canonicalUrl: form.canonicalUrl,
      version: Number(form.version)
    }
    const sentFingerprint = JSON.stringify(payload)
    const article = wasNew
      ? await api.post<ArticleDetail>('/admin/articles', payload)
      : await api.put<ArticleDetail>(`/admin/articles/${id.value}`, payload)
    articleStatus.value = article.status
    if (JSON.stringify(form) === sentFingerprint) {
      applyArticle(article)
      saveState.value = 'saved'
    } else {
      form.version = article.version
      saveState.value = 'dirty'
    }
    savedAt.value = new Date().toLocaleTimeString()
    if (wasNew) await router.replace(`/articles/${article.id}`)
    if (saveState.value === 'dirty') {
      clearTimeout(saveTimer)
      saveTimer = setTimeout(() => void save(true), 500)
    }
    if (!automatic) {
      success.value = editingPublishedArticle.value
        ? editingScheduledArticle.value ? '定时发布内容已更新。' : '公开站文章内容已更新。'
        : '草稿已保存。'
    }
    return article
  } catch (cause) {
    error.value = cause instanceof ApiError ? cause.message : '保存失败'
    saveState.value = 'error'
    return null
  } finally {
    saving.value = false
  }
}

const openPublishConfirmation = () => {
  if (!canWrite.value || editorBusy.value || editingPublishedArticle.value) return
  error.value = ''
  success.value = ''
  const missingFields = [
    ...(!form.title.trim() ? ['标题'] : []),
    ...(!form.slug.trim() ? ['slug'] : [])
  ]
  if (missingFields.length > 0) {
    error.value = `${missingFields.join('和')}不能为空，无法发布`
    return
  }
  if (publishAt.value && Number.isNaN(new Date(publishAt.value).getTime())) {
    error.value = '发布时间格式无效，请重新选择'
    return
  }
  publishConfirmationOpen.value = true
}

const publishArticle = async () => {
  if (!canWrite.value || editorBusy.value || editingPublishedArticle.value) return
  publishing.value = true
  error.value = ''
  success.value = ''
  clearTimeout(saveTimer)
  const requestedPublishAt = publishAt.value
  try {
    const saved = await save(false)
    if (!saved) return
    const selectedDate = requestedPublishAt ? new Date(requestedPublishAt) : null
    const published = await api.post<ArticleDetail>(`/admin/articles/${saved.id}/publish`, {
      publishedAt: selectedDate?.toISOString() ?? null
    })
    applyArticle(published)
    saveState.value = 'saved'
    savedAt.value = new Date().toLocaleTimeString()
    publishConfirmationOpen.value = false
    success.value = published.status === 'SCHEDULED'
      ? `文章已设置为 ${formatApiDate(published.publishedAt)} 定时发布。`
      : form.visibility === 'PUBLIC'
        ? '文章已发布到公开站。'
        : '文章已发布，当前可见性为私密。'
  } catch (cause) {
    success.value = ''
    const detail = cause instanceof ApiError ? cause.message : '发布失败'
    error.value = `草稿已保存，但${detail}`
  } finally {
    publishing.value = false
  }
}

const withdrawArticle = async () => {
  if (!canWrite.value || editorBusy.value || !editingPublishedArticle.value || id.value === null) return
  const wasScheduled = editingScheduledArticle.value
  const hasPendingChanges = saveState.value === 'dirty' || saveState.value === 'error'
  const message = wasScheduled
    ? hasPendingChanges
      ? '将取消定时发布，并把当前编辑内容保存为草稿。确认继续吗？'
      : '确认取消定时发布并转为草稿吗？'
    : hasPendingChanges
      ? '文章将从公开站撤回，当前编辑内容会保存为草稿。确认继续吗？'
      : '确认将这篇文章从公开站撤回并转为草稿吗？'
  if (!window.confirm(message)) return

  withdrawing.value = true
  error.value = ''
  success.value = ''
  clearTimeout(saveTimer)
  try {
    const withdrawn = await api.post<ArticleDetail>(`/admin/articles/${id.value}/withdraw`)
    if (hasPendingChanges) {
      articleStatus.value = withdrawn.status
      form.version = withdrawn.version
      saveState.value = 'dirty'
      const draft = await save(false)
      if (!draft) {
        error.value = `文章已撤回，但当前编辑内容保存失败：${error.value || '请重试保存草稿'}`
        return
      }
    } else {
      applyArticle(withdrawn)
      saveState.value = 'saved'
    }
    savedAt.value = new Date().toLocaleTimeString()
    success.value = wasScheduled
      ? '定时发布已取消，文章已转为草稿。'
      : hasPendingChanges
        ? '文章已撤回，当前编辑内容已保存为草稿。'
        : '文章已撤回并转为草稿。'
  } catch (cause) {
    error.value = cause instanceof ApiError ? cause.message : '撤回失败'
  } finally {
    withdrawing.value = false
  }
}

watch(
  form,
  () => {
    if (!ready.value) return
    if (JSON.stringify(form) === savedFingerprint) return
    success.value = ''
    saveState.value = 'dirty'
    schedulePreview()
    if (!isNew.value && !editingPublishedArticle.value && canWrite.value) {
      clearTimeout(saveTimer)
      saveTimer = setTimeout(() => void save(true), 1800)
    }
  },
  { deep: true }
)

onMounted(load)
onBeforeUnmount(() => {
  clearTimeout(saveTimer)
  clearTimeout(previewTimer)
})
</script>

<template>
  <div class="editor-page">
    <header class="editor-header">
      <div>
        <RouterLink class="back-link" to="/articles">返回文章列表</RouterLink>
        <p class="kicker">CONTENT / {{ isNew ? 'NEW DRAFT' : 'EDIT' }}</p>
      </div>
      <div class="editor-actions">
        <small v-if="saveState === 'dirty'">
          {{ editingPublishedArticle ? '有待确认的线上更新' : '等待自动保存' }}
        </small>
        <small v-else-if="saveState === 'saving'">正在保存…</small>
        <small v-else-if="saveState === 'error'">
          {{ editingPublishedArticle ? '更新发布失败' : '自动保存失败' }}
        </small>
        <small v-else-if="savedAt">已保存于 {{ savedAt }}</small>
        <button
          v-if="editingPublishedArticle"
          class="editor-secondary-action"
          type="button"
          :disabled="editorBusy || !canWrite"
          @click="withdrawArticle"
        >
          {{ withdrawing ? '处理中…' : editingScheduledArticle ? '取消定时发布' : '撤回为草稿' }}
        </button>
        <button
          v-if="editingPublishedArticle"
          class="primary-action"
          type="button"
          :disabled="editorBusy || !canWrite || (saveState !== 'dirty' && saveState !== 'error')"
          @click="save(false)"
        >
          {{ saving ? '更新中…' : editingScheduledArticle ? '更新定时内容' : '更新已发布内容' }}
        </button>
        <button
          v-if="!editingPublishedArticle"
          class="editor-secondary-action"
          type="button"
          :disabled="editorBusy || !canWrite"
          @click="save(false)"
        >
          {{ saving ? '保存中…' : isNew ? '保存草稿' : '保存更改' }}
        </button>
        <button
          v-if="!editingPublishedArticle"
          class="primary-action"
          type="button"
          :disabled="editorBusy || !canWrite"
          @click="openPublishConfirmation"
        >
          {{ publishing ? '发布中…' : '保存并发布' }}
        </button>
      </div>
    </header>

    <p v-if="error" class="page-error">{{ error }}</p>
    <p v-if="success" class="editor-success" role="status">{{ success }}</p>
    <p v-if="editingPublishedArticle && !loading" class="config-notice">
      <template v-if="editingScheduledArticle">
        当前文章计划于 {{ publishTimeName }} 发布。编辑内容不会自动保存，请确认后点击“更新定时内容”。
      </template>
      <template v-else>
        当前文章已发布。编辑内容不会自动同步到公开站；确认无误后，请点击“更新已发布内容”。
      </template>
    </p>
    <section
      v-if="publishConfirmationOpen"
      class="publish-confirmation"
      role="dialog"
      aria-labelledby="publish-confirmation-title"
    >
      <header>
        <div>
          <small>PUBLISH CHECK</small>
          <h2 id="publish-confirmation-title">确认保存并发布</h2>
        </div>
        <button
          type="button"
          aria-label="关闭发布确认"
          :disabled="editorBusy"
          @click="publishConfirmationOpen = false"
        >×</button>
      </header>
      <dl>
        <div>
          <dt>文章</dt>
          <dd>{{ form.title }}</dd>
        </div>
        <div>
          <dt>可见性</dt>
          <dd>{{ visibilityName }}</dd>
        </div>
        <div>
          <dt>分类</dt>
          <dd>{{ selectedCategoryName }}</dd>
        </div>
        <div>
          <dt>评论</dt>
          <dd>{{ form.allowComment ? '允许评论' : '关闭评论' }}</dd>
        </div>
        <div>
          <dt>发布时间</dt>
          <dd>{{ publishTimeName }}</dd>
        </div>
      </dl>
      <p>系统会先保存当前编辑内容；未来时间将进入定时发布，留空则立即发布。</p>
      <footer>
        <button
          type="button"
          class="editor-secondary-action"
          :disabled="editorBusy"
          @click="publishConfirmationOpen = false"
        >继续编辑</button>
        <button
          type="button"
          class="primary-action"
          :disabled="editorBusy"
          @click="publishArticle"
        >{{ publishing ? '发布中…' : '确认保存并发布' }}</button>
      </footer>
    </section>
    <div v-if="loading" class="editor-loading">正在打开稿纸…</div>

    <form v-else class="article-editor" @submit.prevent="save(false)">
      <main>
        <input
          v-model="form.title"
          class="title-input"
          type="text"
          maxlength="200"
          placeholder="文章标题（必填）"
          required
          :readonly="editorLocked"
        >
        <label>
          <span>URL 标识（Slug，必填）</span>
          <div class="slug-field">
            <i>/article/</i>
            <input
              v-model="form.slug"
              type="text"
              maxlength="160"
              pattern="[a-z0-9]+(?:-[a-z0-9]+)*"
              placeholder="my-first-article"
              required
              :readonly="editorLocked"
            >
          </div>
        </label>
        <label>
          <span>摘要</span>
          <textarea
            v-model="form.summary"
            rows="3"
            maxlength="600"
            placeholder="用一两句话告诉读者这篇文章讲什么。"
            :readonly="editorLocked"
          />
        </label>
        <WordLikeEditor
          v-model="form.contentMarkdown"
          :preview-html="previewHtml"
          :previewing="previewing"
          :word-count="previewStats.wordCount"
          :reading-minutes="previewStats.readingMinutes"
          :readonly="editorLocked"
          :image-upload-enabled="ossConfigured"
          :max-image-size="maxImageSize"
          @error="error = $event"
        />
      </main>

      <aside>
        <section>
          <h2>发布设置</h2>
          <label>
            <span>可见性</span>
            <select v-model="form.visibility" :disabled="editorLocked">
              <option value="PUBLIC">公开</option>
              <option value="PRIVATE">私密</option>
            </select>
          </label>
          <label>
            <span>发布时间</span>
            <input
              v-model="publishAt"
              type="datetime-local"
              step="60"
              :readonly="editorLocked || editingPublishedArticle"
            >
            <small v-if="editingScheduledArticle">到达该时间后自动公开发布。</small>
            <small v-else-if="editingPublishedArticle">这是文章当前的发布时间。</small>
            <small v-else>留空立即发布；也可选择过去时间或未来的定时发布时间。</small>
          </label>
          <label>
            <span>分类</span>
            <select v-model="form.categoryId" :disabled="editorLocked">
              <option :value="null">未分类</option>
              <option v-for="item in categories" :key="item.id" :value="item.id">
                {{ item.name }}
              </option>
            </select>
          </label>
          <fieldset>
            <legend>标签</legend>
            <label v-for="item in tags" :key="item.id" class="check-row">
              <input
                v-model="form.tagIds"
                type="checkbox"
                :value="item.id"
                :disabled="editorLocked"
              >
              <span>{{ item.name }}</span>
            </label>
            <small v-if="tags.length === 0">暂无标签，可稍后在标签管理中添加。</small>
          </fieldset>
          <label class="check-row">
            <input v-model="form.pinned" type="checkbox" :disabled="editorLocked">
            <span>置顶文章</span>
          </label>
          <label class="check-row">
            <input v-model="form.allowComment" type="checkbox" :disabled="editorLocked">
            <span>允许评论</span>
          </label>
        </section>

        <section>
          <h2>SEO</h2>
          <label>
            <span>SEO 标题</span>
            <input v-model="form.metaTitle" maxlength="200" :readonly="editorLocked">
          </label>
          <label>
            <span>SEO 描述</span>
            <textarea
              v-model="form.metaDescription"
              rows="4"
              maxlength="320"
              :readonly="editorLocked"
            />
          </label>
          <label>
            <span>Canonical URL</span>
            <input v-model="form.canonicalUrl" maxlength="500" :readonly="editorLocked">
          </label>
        </section>
      </aside>
    </form>
  </div>
</template>

<style scoped>
.editor-actions {
  flex-wrap: wrap;
  justify-content: flex-end;
}

.editor-secondary-action {
  min-height: 38px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 15px;
  color: var(--admin-text);
  border: 1px solid var(--admin-border);
  border-radius: 6px;
  background: var(--admin-surface);
  cursor: pointer;
  font-size: 12px;
  font-weight: 600;
}

.editor-secondary-action:hover:not(:disabled) {
  color: var(--admin-primary);
  border-color: var(--admin-primary);
}

.editor-secondary-action:disabled {
  cursor: not-allowed;
  opacity: .45;
}

.editor-success {
  margin: 0 0 16px;
  padding: 11px 13px;
  color: var(--admin-success);
  border: 1px solid color-mix(in srgb, var(--admin-success) 24%, var(--admin-border));
  border-radius: 6px;
  background: var(--admin-success-soft);
  font-size: 12px;
}

.publish-confirmation {
  display: grid;
  gap: 16px;
  margin: 0 0 18px;
  padding: 18px;
  color: var(--admin-text);
  border: 1px solid color-mix(in srgb, var(--admin-primary) 34%, var(--admin-border));
  border-radius: 10px;
  background: color-mix(in srgb, var(--admin-primary) 5%, var(--admin-surface));
  box-shadow: 0 12px 30px rgb(8 24 42 / 8%);
}

.publish-confirmation > header,
.publish-confirmation > footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.publish-confirmation header small {
  color: var(--admin-primary);
  font-size: 9px;
  font-weight: 700;
  letter-spacing: .14em;
}

.publish-confirmation h2 {
  margin: 3px 0 0;
  font-size: 18px;
}

.publish-confirmation header > button {
  width: 32px;
  height: 32px;
  display: grid;
  padding: 0;
  place-items: center;
  color: var(--admin-subtle);
  border: 1px solid var(--admin-border);
  border-radius: 50%;
  background: var(--admin-surface);
  cursor: pointer;
  font-size: 20px;
}

.publish-confirmation dl {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(130px, 1fr));
  gap: 10px;
  margin: 0;
}

.publish-confirmation dl > div {
  min-width: 0;
  padding: 11px 12px;
  border: 1px solid var(--admin-divider);
  border-radius: 7px;
  background: var(--admin-surface);
}

.publish-confirmation dt {
  margin-bottom: 4px;
  color: var(--admin-subtle);
  font-size: 10px;
}

.publish-confirmation dd {
  margin: 0;
  overflow: hidden;
  font-size: 12px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.publish-confirmation > p {
  margin: 0;
  color: var(--admin-subtle);
  font-size: 11px;
}

.publish-confirmation > footer {
  justify-content: flex-end;
}

@media (max-width: 760px) {
  .editor-actions {
    justify-content: flex-start;
  }

  .publish-confirmation dl {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
