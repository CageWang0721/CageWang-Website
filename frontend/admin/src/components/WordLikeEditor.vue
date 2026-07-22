<script setup lang="ts">
import { computed, nextTick, ref } from 'vue'

import { ApiError } from '@personal-blog/api-client'

import { api } from '../services/api'
import type { MediaAsset } from '../types/media'

type ImageAlignment = 'left' | 'center' | 'right'
type ImageSize = 'small' | 'medium' | 'large' | 'full'

type ImageMatch = {
  start: number
  end: number
  src: string
  alt: string
  caption: string
  alignment: ImageAlignment
  size: ImageSize
}

const props = defineProps<{
  modelValue: string
  previewHtml: string
  previewing: boolean
  wordCount: number
  readingMinutes: number
  readonly: boolean
  imageUploadEnabled: boolean
  maxImageSize?: number
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
  error: [message: string]
}>()

const textarea = ref<HTMLTextAreaElement | null>(null)
const imageInput = ref<HTMLInputElement | null>(null)
const fullscreen = ref(false)
const imagePanelOpen = ref(false)
const imageUploading = ref(false)
const editingImage = ref<ImageMatch | null>(null)
const imageUrl = ref('')
const imageAlt = ref('')
const imageCaption = ref('')
const imageAlignment = ref<ImageAlignment>('center')
const imageSize = ref<ImageSize>('large')

const lineCount = computed(() => props.modelValue.length === 0
  ? 1
  : props.modelValue.split('\n').length)

const updateValue = (value: string) => emit('update:modelValue', value)

const focusRange = async (start: number, end = start) => {
  await nextTick()
  textarea.value?.focus()
  textarea.value?.setSelectionRange(start, end)
}

const replaceRange = (
  start: number,
  end: number,
  replacement: string,
  selectionStart = start + replacement.length,
  selectionEnd = selectionStart
) => {
  updateValue(`${props.modelValue.slice(0, start)}${replacement}${props.modelValue.slice(end)}`)
  void focusRange(selectionStart, selectionEnd)
}

const selectedRange = () => {
  const element = textarea.value
  const fallback = props.modelValue.length
  return {
    start: element?.selectionStart ?? fallback,
    end: element?.selectionEnd ?? fallback
  }
}

const wrapSelection = (before: string, after: string, placeholder: string) => {
  if (props.readonly) return
  const { start, end } = selectedRange()
  const selected = props.modelValue.slice(start, end) || placeholder
  const replacement = `${before}${selected}${after}`
  replaceRange(start, end, replacement, start + before.length, start + before.length + selected.length)
}

const transformSelectedLines = (transform: (line: string, index: number) => string) => {
  if (props.readonly) return
  const { start, end } = selectedRange()
  const lineStart = props.modelValue.lastIndexOf('\n', Math.max(0, start - 1)) + 1
  const nextBreak = props.modelValue.indexOf('\n', end)
  const lineEnd = nextBreak === -1 ? props.modelValue.length : nextBreak
  const original = props.modelValue.slice(lineStart, lineEnd)
  const replacement = original.split('\n').map(transform).join('\n')
  replaceRange(lineStart, lineEnd, replacement, lineStart, lineStart + replacement.length)
}

const applyHeading = (event: Event) => {
  const level = (event.target as HTMLSelectElement).value
  ;(event.target as HTMLSelectElement).value = ''
  if (!level) return
  transformSelectedLines((line) => {
    const content = line.replace(/^#{1,6}\s+/, '')
    return level === 'paragraph' ? content : `${'#'.repeat(Number(level))} ${content}`
  })
}

const applyInlineClass = (event: Event, prefix: string, placeholder = '文字') => {
  const select = event.target as HTMLSelectElement
  const value = select.value
  select.value = ''
  if (!value) return
  wrapSelection(`<span class="${prefix}${value}">`, '</span>', placeholder)
}

const toggleLinePrefix = (prefix: string) => {
  const { start, end } = selectedRange()
  const lineStart = props.modelValue.lastIndexOf('\n', Math.max(0, start - 1)) + 1
  const nextBreak = props.modelValue.indexOf('\n', end)
  const lineEnd = nextBreak === -1 ? props.modelValue.length : nextBreak
  const lines = props.modelValue.slice(lineStart, lineEnd).split('\n')
  const allPrefixed = lines.every((line) => line.startsWith(prefix) || line.trim() === '')
  const replacement = lines.map((line) => {
    if (!line.trim()) return line
    return allPrefixed ? line.slice(prefix.length) : `${prefix}${line}`
  }).join('\n')
  replaceRange(lineStart, lineEnd, replacement, lineStart, lineStart + replacement.length)
}

const applyOrderedList = () => {
  transformSelectedLines((line, index) => line.trim() ? `${index + 1}. ${line.replace(/^\d+\.\s+/, '')}` : line)
}

const alignSelection = (alignment: ImageAlignment) => {
  const labels: Record<ImageAlignment, string> = {
    left: '左对齐文字',
    center: '居中文字',
    right: '右对齐文字'
  }
  wrapSelection(`<span class="wc-align--${alignment}">`, '</span>', labels[alignment])
}

const insertLink = () => {
  if (props.readonly) return
  const { start, end } = selectedRange()
  const selected = props.modelValue.slice(start, end)
  const url = window.prompt('输入链接地址（https://…）', 'https://')?.trim()
  if (!url) return
  const label = selected || window.prompt('输入链接文字', '链接文字')?.trim() || url
  replaceRange(start, end, `[${label}](${url})`, start + 1, start + 1 + label.length)
}

const insertCodeBlock = () => {
  const { start, end } = selectedRange()
  const selected = props.modelValue.slice(start, end) || '在这里输入代码'
  const replacement = `\n\n\`\`\`\n${selected}\n\`\`\`\n\n`
  replaceRange(start, end, replacement, start + 5, start + 5 + selected.length)
}

const insertTable = () => {
  const table = '| 列 1 | 列 2 | 列 3 |\n| --- | --- | --- |\n| 内容 | 内容 | 内容 |'
  const { start, end } = selectedRange()
  const prefix = start > 0 && !props.modelValue.slice(0, start).endsWith('\n\n') ? '\n\n' : ''
  replaceRange(start, end, `${prefix}${table}\n\n`, start + prefix.length, start + prefix.length + table.length)
}

const insertDivider = () => {
  const { start, end } = selectedRange()
  replaceRange(start, end, '\n\n---\n\n')
}

const clearFormatting = () => {
  if (props.readonly) return
  const { start, end } = selectedRange()
  if (start === end) return
  const selected = props.modelValue.slice(start, end)
  const cleaned = selected
    .replace(/<\/?(?:span|u|mark|sup|sub)\b[^>]*>/gi, '')
    .replace(/(\*\*|__|~~|`)/g, '')
    .replace(/^#{1,6}\s+/gm, '')
    .replace(/^>\s?/gm, '')
  replaceRange(start, end, cleaned, start, start + cleaned.length)
}

const decodeHtml = (value: string) => {
  const helper = document.createElement('textarea')
  helper.innerHTML = value
  return helper.value
}

const escapeHtml = (value: string) => value
  .replace(/&/g, '&amp;')
  .replace(/</g, '&lt;')
  .replace(/>/g, '&gt;')
  .replace(/"/g, '&quot;')

const imageMatches = (): ImageMatch[] => {
  const matches: ImageMatch[] = []
  const figurePattern = /<figure class="([^"]*\bwc-image\b[^"]*)">\s*<img src="([^"]*)" alt="([^"]*)">\s*(?:<figcaption>([\s\S]*?)<\/figcaption>\s*)?<\/figure>/g
  let figure: RegExpExecArray | null
  while ((figure = figurePattern.exec(props.modelValue)) !== null) {
    const classes = figure[1].split(/\s+/)
    matches.push({
      start: figure.index,
      end: figure.index + figure[0].length,
      src: decodeHtml(figure[2]),
      alt: decodeHtml(figure[3]),
      caption: decodeHtml(figure[4] || ''),
      alignment: classes.includes('wc-image--left') ? 'left' : classes.includes('wc-image--right') ? 'right' : 'center',
      size: classes.includes('wc-image--small')
        ? 'small'
        : classes.includes('wc-image--medium')
          ? 'medium'
          : classes.includes('wc-image--full') ? 'full' : 'large'
    })
  }

  const markdownPattern = /!\[([^\]]*)\]\((\S+?)(?:\s+"([^"]*)")?\)/g
  let markdown: RegExpExecArray | null
  while ((markdown = markdownPattern.exec(props.modelValue)) !== null) {
    const overlapsFigure = matches.some((match) => markdown!.index >= match.start && markdown!.index < match.end)
    if (!overlapsFigure) {
      matches.push({
        start: markdown.index,
        end: markdown.index + markdown[0].length,
        src: markdown[2],
        alt: markdown[1],
        caption: markdown[3] || '',
        alignment: 'center',
        size: 'large'
      })
    }
  }
  return matches.sort((a, b) => a.start - b.start)
}

const resetImageForm = () => {
  editingImage.value = null
  imageUrl.value = ''
  imageAlt.value = ''
  imageCaption.value = ''
  imageAlignment.value = 'center'
  imageSize.value = 'large'
}

const fillImageForm = (match: ImageMatch) => {
  editingImage.value = match
  imageUrl.value = match.src
  imageAlt.value = match.alt
  imageCaption.value = match.caption
  imageAlignment.value = match.alignment
  imageSize.value = match.size
}

const openImagePanel = () => {
  if (props.readonly) return
  const caret = selectedRange().start
  const match = imageMatches().find((item) => caret >= item.start && caret <= item.end)
  if (match) fillImageForm(match)
  else resetImageForm()
  imagePanelOpen.value = true
}

const closeImagePanel = () => {
  if (imageUploading.value) return
  imagePanelOpen.value = false
  resetImageForm()
}

const chooseImageFile = () => imageInput.value?.click()

const uploadImage = async (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file || imageUploading.value) return
  if (props.maxImageSize && file.size > props.maxImageSize) {
    emit('error', `图片不能超过 ${Math.round(props.maxImageSize / 1024 / 1024)} MB`)
    return
  }

  const alt = file.name.replace(/\.[^.]+$/, '')
  const body = new FormData()
  body.append('file', file)
  body.append('altText', alt)
  imageUploading.value = true
  try {
    const asset = await api.postForm<MediaAsset>('/admin/media', body)
    imageUrl.value = asset.url
    if (!imageAlt.value) imageAlt.value = asset.altText || alt
  } catch (cause) {
    emit('error', cause instanceof ApiError ? cause.message : '图片上传失败')
  } finally {
    imageUploading.value = false
  }
}

const saveImage = () => {
  const src = imageUrl.value.trim()
  if (!src) {
    emit('error', '请先上传图片或填写图片地址')
    return
  }
  if (/^(?:javascript|data|vbscript):/i.test(src)) {
    emit('error', '图片地址必须使用站内路径或 HTTP(S) 地址')
    return
  }

  const caption = imageCaption.value.trim()
  const markup = [
    `<figure class="wc-image wc-image--${imageAlignment.value} wc-image--${imageSize.value}">`,
    `  <img src="${escapeHtml(src)}" alt="${escapeHtml(imageAlt.value.trim())}">`,
    ...(caption ? [`  <figcaption>${escapeHtml(caption)}</figcaption>`] : []),
    '</figure>'
  ].join('\n')

  if (editingImage.value) {
    replaceRange(editingImage.value.start, editingImage.value.end, markup)
  } else {
    const { start, end } = selectedRange()
    const before = start > 0 && !props.modelValue.slice(0, start).endsWith('\n\n') ? '\n\n' : ''
    const after = end < props.modelValue.length && !props.modelValue.slice(end).startsWith('\n\n') ? '\n\n' : '\n'
    replaceRange(start, end, `${before}${markup}${after}`)
  }
  imagePanelOpen.value = false
  resetImageForm()
}

const removeImage = () => {
  if (!editingImage.value) return
  replaceRange(editingImage.value.start, editingImage.value.end, '')
  imagePanelOpen.value = false
  resetImageForm()
}

const editPreviewImage = (event: MouseEvent) => {
  if (props.readonly) return
  const target = event.target as HTMLElement
  const image = target.closest('img')
  if (!(image instanceof HTMLImageElement)) return
  const src = image.getAttribute('src') || ''
  const match = imageMatches().find((item) => item.src === src || image.src.endsWith(item.src))
  if (!match) return
  fillImageForm(match)
  imagePanelOpen.value = true
}

const handleShortcut = (event: KeyboardEvent) => {
  if (!(event.ctrlKey || event.metaKey) || props.readonly) return
  const key = event.key.toLowerCase()
  if (key === 'b') {
    event.preventDefault()
    wrapSelection('**', '**', '粗体文字')
  } else if (key === 'i') {
    event.preventDefault()
    wrapSelection('*', '*', '斜体文字')
  } else if (key === 'u') {
    event.preventDefault()
    wrapSelection('<u>', '</u>', '下划线文字')
  } else if (key === 'k') {
    event.preventDefault()
    insertLink()
  }
}
</script>

<template>
  <section class="word-editor" :class="{ 'is-fullscreen': fullscreen }">
    <div class="editor-ribbon">
      <div class="ribbon-heading">
        <div class="ribbon-tabs">
          <b>开始</b>
          <span>插入与排版</span>
        </div>
        <div class="ribbon-heading-actions">
          <span>选中文字后使用工具，快捷键支持 Ctrl / ⌘ + B、I、U、K</span>
          <button
            type="button"
            class="fullscreen-button"
            :aria-label="fullscreen ? '退出全屏编辑' : '全屏编辑'"
            :title="fullscreen ? '退出全屏' : '全屏编辑'"
            @click="fullscreen = !fullscreen"
          >
            {{ fullscreen ? '退出全屏' : '全屏' }}
          </button>
        </div>
      </div>

      <div class="ribbon-tools" aria-label="正文格式工具栏">
        <div class="tool-group tool-group-selects">
          <select :disabled="readonly" aria-label="段落样式" title="段落样式" @change="applyHeading">
            <option value="">正文样式</option>
            <option value="paragraph">正文</option>
            <option value="1">标题 1</option>
            <option value="2">标题 2</option>
            <option value="3">标题 3</option>
          </select>
          <select
            :disabled="readonly"
            aria-label="字体"
            title="字体"
            @change="applyInlineClass($event, 'wc-font--')"
          >
            <option value="">字体</option>
            <option value="sans">无衬线</option>
            <option value="serif">衬线</option>
            <option value="mono">等宽</option>
          </select>
          <select
            :disabled="readonly"
            aria-label="字号"
            title="字号"
            @change="applyInlineClass($event, 'wc-size--')"
          >
            <option value="">字号</option>
            <option value="small">小</option>
            <option value="normal">标准</option>
            <option value="large">大</option>
            <option value="xlarge">特大</option>
          </select>
        </div>

        <div class="tool-group compact-tools">
          <button type="button" :disabled="readonly" title="粗体 (Ctrl+B)" aria-label="粗体" @click="wrapSelection('**', '**', '粗体文字')"><b>B</b></button>
          <button type="button" :disabled="readonly" title="斜体 (Ctrl+I)" aria-label="斜体" @click="wrapSelection('*', '*', '斜体文字')"><i>I</i></button>
          <button type="button" :disabled="readonly" title="下划线 (Ctrl+U)" aria-label="下划线" @click="wrapSelection('<u>', '</u>', '下划线文字')"><u>U</u></button>
          <button type="button" :disabled="readonly" title="删除线" aria-label="删除线" @click="wrapSelection('~~', '~~', '删除线文字')"><s>S</s></button>
          <select
            class="color-select"
            :disabled="readonly"
            aria-label="文字颜色"
            title="文字颜色"
            @change="applyInlineClass($event, 'wc-color--')"
          >
            <option value="">A⌄</option>
            <option value="accent">强调色</option>
            <option value="red">红色</option>
            <option value="blue">蓝色</option>
            <option value="green">绿色</option>
            <option value="muted">灰色</option>
          </select>
          <select
            class="color-select"
            :disabled="readonly"
            aria-label="文字高亮"
            title="文字高亮"
            @change="applyInlineClass($event, 'wc-highlight--')"
          >
            <option value="">荧光笔</option>
            <option value="yellow">黄色</option>
            <option value="green">绿色</option>
            <option value="pink">粉色</option>
          </select>
          <button type="button" :disabled="readonly" title="清除所选格式" aria-label="清除格式" @click="clearFormatting">清除</button>
        </div>

        <div class="tool-group compact-tools">
          <button type="button" :disabled="readonly" title="左对齐" aria-label="左对齐" @click="alignSelection('left')">≡</button>
          <button type="button" :disabled="readonly" title="居中" aria-label="居中" @click="alignSelection('center')">≣</button>
          <button type="button" :disabled="readonly" title="右对齐" aria-label="右对齐" @click="alignSelection('right')">≡</button>
          <button type="button" :disabled="readonly" title="引用" aria-label="引用" @click="toggleLinePrefix('> ')">❝</button>
          <button type="button" :disabled="readonly" title="项目符号" aria-label="项目符号" @click="toggleLinePrefix('- ')">• 列表</button>
          <button type="button" :disabled="readonly" title="编号列表" aria-label="编号列表" @click="applyOrderedList">1. 列表</button>
        </div>

        <div class="tool-group insert-tools">
          <button type="button" :disabled="readonly" title="插入链接 (Ctrl+K)" @click="insertLink"><span>↗</span>链接</button>
          <button type="button" :disabled="readonly" title="插入或调整图片" @click="openImagePanel"><span>▧</span>图片</button>
          <button type="button" :disabled="readonly" title="插入表格" @click="insertTable"><span>▦</span>表格</button>
          <button type="button" :disabled="readonly" title="插入代码块" @click="insertCodeBlock"><span>&lt;/&gt;</span>代码</button>
          <button type="button" :disabled="readonly" title="插入分隔线" @click="insertDivider"><span>―</span>分隔线</button>
        </div>
      </div>
    </div>

    <div class="editor-canvas">
      <label class="source-pane">
        <span class="pane-title">
          <b>正文</b>
          <small>Markdown 源文</small>
        </span>
        <textarea
          ref="textarea"
          :value="modelValue"
          rows="24"
          placeholder="从这里开始写作…选中文字即可使用上方格式工具。"
          :readonly="readonly"
          spellcheck="true"
          @input="updateValue(($event.target as HTMLTextAreaElement).value)"
          @keydown="handleShortcut"
        />
      </label>

      <section class="preview-pane">
        <div class="pane-title">
          <b>实时预览</b>
          <small>{{ previewing ? '渲染中…' : `${wordCount} 字 · 约 ${readingMinutes} 分钟` }}</small>
        </div>
        <!-- HTML is rendered and sanitized by the backend Markdown service. -->
        <article v-if="previewHtml" @click="editPreviewImage" v-html="previewHtml" />
        <div v-else class="preview-empty">
          <span>✦</span>
          <p>预览会在你开始写作后出现</p>
        </div>
      </section>
    </div>

    <footer class="editor-statusbar">
      <span>Markdown</span>
      <span>{{ lineCount }} 行</span>
      <span>{{ modelValue.length }} 个字符</span>
      <span class="status-tip">提示：点击右侧预览中的图片可再次调整</span>
    </footer>

    <div v-if="imagePanelOpen" class="image-panel-backdrop" @mousedown.self="closeImagePanel">
      <section class="image-panel" role="dialog" aria-modal="true" aria-labelledby="image-panel-title">
        <header>
          <div>
            <small>IMAGE TOOLS</small>
            <h2 id="image-panel-title">{{ editingImage ? '调整图片' : '插入图片' }}</h2>
          </div>
          <button type="button" aria-label="关闭图片设置" :disabled="imageUploading" @click="closeImagePanel">×</button>
        </header>

        <div class="image-panel-body">
          <div class="image-source-card">
            <div class="image-stage" :class="`align-${imageAlignment}`">
              <img v-if="imageUrl" :src="imageUrl" :alt="imageAlt">
              <div v-else class="image-placeholder">
                <span>▧</span>
                <p>上传图片或粘贴图片地址</p>
              </div>
            </div>
            <input
              ref="imageInput"
              class="visually-hidden"
              type="file"
              accept="image/jpeg,image/png,image/webp,image/gif"
              @change="uploadImage"
            >
            <button
              type="button"
              class="upload-button"
              :disabled="imageUploading || !imageUploadEnabled"
              @click="chooseImageFile"
            >
              {{ imageUploading ? '上传中…' : imageUploadEnabled ? '从电脑上传' : '媒体存储未配置' }}
            </button>
          </div>

          <div class="image-fields">
            <label>
              <span>图片地址</span>
              <input v-model="imageUrl" type="text" placeholder="https://… 或 /images/…">
            </label>
            <label>
              <span>替代文本 <small>帮助无障碍阅读与 SEO</small></span>
              <input v-model="imageAlt" type="text" maxlength="300" placeholder="描述图片内容">
            </label>
            <label>
              <span>图片说明 <small>可选，显示在图片下方</small></span>
              <input v-model="imageCaption" type="text" maxlength="500" placeholder="图片来源或补充说明">
            </label>

            <fieldset>
              <legend>显示尺寸</legend>
              <div class="segmented-control size-control">
                <label><input v-model="imageSize" type="radio" value="small"><span>小</span></label>
                <label><input v-model="imageSize" type="radio" value="medium"><span>中</span></label>
                <label><input v-model="imageSize" type="radio" value="large"><span>大</span></label>
                <label><input v-model="imageSize" type="radio" value="full"><span>通栏</span></label>
              </div>
            </fieldset>

            <fieldset>
              <legend>对齐方式</legend>
              <div class="segmented-control">
                <label><input v-model="imageAlignment" type="radio" value="left"><span>左对齐</span></label>
                <label><input v-model="imageAlignment" type="radio" value="center"><span>居中</span></label>
                <label><input v-model="imageAlignment" type="radio" value="right"><span>右对齐</span></label>
              </div>
            </fieldset>
          </div>
        </div>

        <footer>
          <button v-if="editingImage" type="button" class="remove-image" :disabled="imageUploading" @click="removeImage">删除图片</button>
          <span />
          <button type="button" :disabled="imageUploading" @click="closeImagePanel">取消</button>
          <button type="button" class="save-image" :disabled="imageUploading" @click="saveImage">
            {{ editingImage ? '应用调整' : '插入正文' }}
          </button>
        </footer>
      </section>
    </div>
  </section>
</template>

<style scoped>
.word-editor {
  min-width: 0;
  overflow: hidden;
  border: 1px solid var(--admin-border);
  border-radius: 10px;
  background: var(--admin-surface);
  box-shadow: 0 1px 2px rgb(15 23 42 / 4%);
}

.word-editor.is-fullscreen {
  position: fixed;
  z-index: 1200;
  inset: 0;
  display: flex;
  flex-direction: column;
  border: 0;
  border-radius: 0;
}

.editor-ribbon {
  position: relative;
  z-index: 2;
  border-bottom: 1px solid var(--admin-border);
  background: color-mix(in srgb, var(--admin-surface) 94%, var(--admin-accent) 6%);
}

.ribbon-heading {
  min-height: 40px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 0 12px;
  border-bottom: 1px solid var(--admin-divider);
}

.ribbon-tabs,
.ribbon-heading-actions,
.ribbon-tools,
.tool-group,
.editor-statusbar,
.pane-title,
.image-panel header,
.image-panel footer {
  display: flex;
  align-items: center;
}

.ribbon-tabs {
  align-self: stretch;
  gap: 22px;
}

.ribbon-tabs b {
  height: 100%;
  display: grid;
  place-items: center;
  color: var(--admin-accent);
  border-bottom: 2px solid var(--admin-accent);
  font-size: 12px;
}

.ribbon-tabs span,
.ribbon-heading-actions span {
  color: var(--admin-subtle);
  font-size: 11px;
}

.ribbon-heading-actions {
  gap: 12px;
}

.fullscreen-button {
  min-height: 28px !important;
  padding: 0 9px !important;
  border: 1px solid var(--admin-border) !important;
  border-radius: 5px !important;
  background: var(--admin-surface) !important;
  color: var(--admin-text) !important;
  font-size: 11px !important;
}

.ribbon-tools {
  min-height: 82px;
  gap: 0;
  padding: 8px;
  overflow-x: auto;
}

.tool-group {
  min-height: 62px;
  flex: 0 0 auto;
  align-content: center;
  gap: 4px;
  padding: 0 10px;
  border-right: 1px solid var(--admin-divider);
}

.tool-group:first-child {
  padding-left: 2px;
}

.tool-group:last-child {
  border-right: 0;
}

.tool-group-selects {
  width: 230px;
  display: grid;
  grid-template-columns: 1fr 76px;
}

.tool-group-selects select:first-child {
  grid-column: 1 / -1;
}

.ribbon-tools select,
.ribbon-tools button {
  min-height: 28px;
  margin: 0;
  padding: 0 8px;
  color: var(--admin-text);
  border: 1px solid transparent;
  border-radius: 4px;
  background: transparent;
  box-shadow: none;
  font: 11px/1.2 -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
  white-space: nowrap;
}

.ribbon-tools select {
  width: 100%;
  border-color: var(--admin-border);
  background: var(--admin-surface);
}

.ribbon-tools button:hover:not(:disabled),
.ribbon-tools select:hover:not(:disabled) {
  border-color: color-mix(in srgb, var(--admin-accent) 35%, var(--admin-border));
  background: color-mix(in srgb, var(--admin-accent) 9%, var(--admin-surface));
}

.ribbon-tools button:disabled,
.ribbon-tools select:disabled {
  opacity: .45;
}

.compact-tools {
  max-width: 224px;
  flex-wrap: wrap;
}

.compact-tools button {
  min-width: 30px;
}

.compact-tools .color-select {
  width: auto;
  max-width: 72px;
}

.insert-tools button {
  min-width: 54px;
  min-height: 52px;
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 5px;
}

.insert-tools button span {
  color: var(--admin-accent);
  font-size: 17px;
  line-height: 1;
}

.editor-canvas {
  min-height: 520px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  background: color-mix(in srgb, var(--admin-page) 78%, var(--admin-surface));
}

.is-fullscreen .editor-canvas {
  min-height: 0;
  flex: 1;
}

.source-pane,
.preview-pane {
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  margin: 0;
}

.source-pane {
  border-right: 1px solid var(--admin-border);
}

.pane-title {
  min-height: 38px;
  justify-content: space-between;
  gap: 12px;
  padding: 0 14px;
  color: var(--admin-subtle);
  border-bottom: 1px solid var(--admin-divider);
  background: var(--admin-surface);
}

.pane-title b {
  color: var(--admin-text);
  font-size: 11px;
  letter-spacing: .04em;
}

.pane-title small {
  font-size: 10px;
}

.source-pane textarea {
  width: 100%;
  min-height: 520px;
  flex: 1;
  margin: 0;
  padding: 24px 28px;
  resize: vertical;
  color: var(--admin-text);
  border: 0 !important;
  border-radius: 0 !important;
  outline: 0;
  background: var(--admin-surface) !important;
  box-shadow: none !important;
  font: 14px/1.85 "Cascadia Code", "SFMono-Regular", Consolas, monospace;
  tab-size: 2;
}

.is-fullscreen .source-pane textarea {
  min-height: 0;
  resize: none;
}

.preview-pane {
  max-height: 760px;
  overflow: auto;
  background: var(--admin-surface);
}

.is-fullscreen .preview-pane {
  max-height: none;
}

.preview-pane > article {
  padding: 26px 32px 60px;
  color: var(--admin-text);
  font: 16px/1.9 Georgia, "Noto Serif SC", "Songti SC", serif;
  overflow-wrap: anywhere;
}

.preview-pane > article :deep(:first-child) {
  margin-top: 0;
}

.preview-pane > article :deep(h1),
.preview-pane > article :deep(h2),
.preview-pane > article :deep(h3) {
  margin: 1.8em 0 .7em;
  color: var(--admin-text);
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", sans-serif;
  line-height: 1.35;
}

.preview-pane > article :deep(h1) { font-size: 28px; }
.preview-pane > article :deep(h2) { padding-bottom: 8px; border-bottom: 1px solid var(--admin-divider); font-size: 23px; }
.preview-pane > article :deep(h3) { font-size: 19px; }
.preview-pane > article :deep(p) { margin: 1.25em 0; }
.preview-pane > article :deep(a) { color: var(--admin-accent); text-decoration: underline; text-underline-offset: 3px; }
.preview-pane > article :deep(blockquote) { margin: 1.6em 0; padding: 4px 0 4px 18px; color: var(--admin-subtle); border-left: 3px solid var(--admin-accent); }
.preview-pane > article :deep(pre) { padding: 16px; overflow: auto; color: #e8edf2; border-radius: 6px; background: #1d2530; font: 12px/1.7 Consolas, monospace; }
.preview-pane > article :deep(table) { width: 100%; margin: 1.5em 0; border-collapse: collapse; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; font-size: 13px; }
.preview-pane > article :deep(th),
.preview-pane > article :deep(td) { padding: 9px 10px; border: 1px solid var(--admin-divider); text-align: left; }

.preview-pane > article :deep(img) {
  max-width: 100%;
  height: auto;
  cursor: pointer;
}

.preview-pane > article :deep(img:hover) {
  outline: 3px solid color-mix(in srgb, var(--admin-accent) 45%, transparent);
  outline-offset: 3px;
}

.preview-pane > article :deep(.wc-font--sans) { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", sans-serif; }
.preview-pane > article :deep(.wc-font--serif) { font-family: Georgia, "Noto Serif SC", "Songti SC", serif; }
.preview-pane > article :deep(.wc-font--mono) { font-family: "Cascadia Code", Consolas, monospace; }
.preview-pane > article :deep(.wc-size--small) { font-size: .86em; }
.preview-pane > article :deep(.wc-size--normal) { font-size: 1em; }
.preview-pane > article :deep(.wc-size--large) { font-size: 1.22em; }
.preview-pane > article :deep(.wc-size--xlarge) { font-size: 1.55em; line-height: 1.5; }
.preview-pane > article :deep(.wc-color--accent) { color: var(--admin-accent); }
.preview-pane > article :deep(.wc-color--red) { color: #c83b3b; }
.preview-pane > article :deep(.wc-color--blue) { color: #2563b8; }
.preview-pane > article :deep(.wc-color--green) { color: #278153; }
.preview-pane > article :deep(.wc-color--muted) { color: var(--admin-subtle); }
.preview-pane > article :deep(.wc-highlight--yellow) { padding: 0 .12em; background: #ffe98b; }
.preview-pane > article :deep(.wc-highlight--green) { padding: 0 .12em; background: #bcebc9; }
.preview-pane > article :deep(.wc-highlight--pink) { padding: 0 .12em; background: #ffd1dd; }
.preview-pane > article :deep(.wc-align--left),
.preview-pane > article :deep(.wc-align--center),
.preview-pane > article :deep(.wc-align--right) { display: block; width: 100%; }
.preview-pane > article :deep(.wc-align--left) { text-align: left; }
.preview-pane > article :deep(.wc-align--center) { text-align: center; }
.preview-pane > article :deep(.wc-align--right) { text-align: right; }

.preview-pane > article :deep(.wc-image) {
  width: 100%;
  max-width: 100%;
  margin: 2em auto;
}

.preview-pane > article :deep(.wc-image--small) { width: min(100%, 260px); }
.preview-pane > article :deep(.wc-image--medium) { width: min(100%, 440px); }
.preview-pane > article :deep(.wc-image--large) { width: min(100%, 680px); }
.preview-pane > article :deep(.wc-image--full) { width: 100%; }
.preview-pane > article :deep(.wc-image--left) { margin-left: 0; margin-right: auto; }
.preview-pane > article :deep(.wc-image--center) { margin-inline: auto; }
.preview-pane > article :deep(.wc-image--right) { margin-left: auto; margin-right: 0; }
.preview-pane > article :deep(.wc-image img) { width: 100%; display: block; margin: 0; border-radius: 6px; }
.preview-pane > article :deep(.wc-image figcaption) { margin-top: 9px; color: var(--admin-subtle); font: 11px/1.6 -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; text-align: center; }

.preview-empty {
  min-height: 430px;
  display: grid;
  place-content: center;
  color: var(--admin-subtle);
  text-align: center;
}

.preview-empty span {
  color: var(--admin-accent);
  font-size: 25px;
}

.preview-empty p {
  margin: 8px 0 0;
  font-size: 12px;
}

.editor-statusbar {
  min-height: 28px;
  gap: 18px;
  padding: 0 12px;
  color: var(--admin-subtle);
  border-top: 1px solid var(--admin-border);
  background: color-mix(in srgb, var(--admin-surface) 92%, var(--admin-page));
  font-size: 10px;
}

.status-tip {
  margin-left: auto;
}

.image-panel-backdrop {
  position: fixed;
  z-index: 1400;
  inset: 0;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgb(10 18 28 / 50%);
  backdrop-filter: blur(3px);
}

.image-panel {
  width: min(820px, 100%);
  max-height: min(720px, calc(100vh - 48px));
  overflow: auto;
  color: var(--admin-text);
  border: 1px solid var(--admin-border);
  border-radius: 12px;
  background: var(--admin-surface);
  box-shadow: 0 24px 70px rgb(0 0 0 / 28%);
}

.image-panel header {
  position: sticky;
  z-index: 2;
  top: 0;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 20px;
  border-bottom: 1px solid var(--admin-divider);
  background: var(--admin-surface);
}

.image-panel header small {
  color: var(--admin-accent);
  font-size: 9px;
  font-weight: 700;
  letter-spacing: .16em;
}

.image-panel h2 {
  margin: 3px 0 0;
  font-size: 19px;
}

.image-panel header button {
  width: 32px;
  height: 32px;
  display: grid;
  padding: 0;
  place-items: center;
  color: var(--admin-subtle);
  border: 1px solid var(--admin-border);
  border-radius: 50%;
  background: transparent;
  font-size: 21px;
}

.image-panel-body {
  display: grid;
  grid-template-columns: minmax(240px, .9fr) minmax(300px, 1.1fr);
  gap: 24px;
  padding: 22px;
}

.image-source-card {
  min-width: 0;
}

.image-stage {
  min-height: 280px;
  display: flex;
  padding: 20px;
  align-items: center;
  overflow: hidden;
  border: 1px dashed var(--admin-border);
  border-radius: 8px;
  background:
    linear-gradient(45deg, color-mix(in srgb, var(--admin-page) 70%, transparent) 25%, transparent 25%) 0 0 / 18px 18px,
    linear-gradient(-45deg, color-mix(in srgb, var(--admin-page) 70%, transparent) 25%, transparent 25%) 0 9px / 18px 18px,
    var(--admin-surface);
}

.image-stage.align-left { justify-content: flex-start; }
.image-stage.align-center { justify-content: center; }
.image-stage.align-right { justify-content: flex-end; }

.image-stage img {
  max-width: 100%;
  max-height: 300px;
  object-fit: contain;
  border-radius: 5px;
  box-shadow: 0 8px 25px rgb(0 0 0 / 13%);
}

.image-placeholder {
  width: 100%;
  color: var(--admin-subtle);
  text-align: center;
}

.image-placeholder span {
  color: var(--admin-accent);
  font-size: 32px;
}

.image-placeholder p {
  margin: 8px 0 0;
  font-size: 11px;
}

.upload-button {
  width: 100%;
  min-height: 36px;
  margin-top: 10px;
  color: var(--admin-accent);
  border: 1px solid color-mix(in srgb, var(--admin-accent) 35%, var(--admin-border));
  border-radius: 6px;
  background: color-mix(in srgb, var(--admin-accent) 7%, var(--admin-surface));
}

.image-fields {
  display: grid;
  align-content: start;
  gap: 14px;
}

.image-fields > label {
  display: grid;
  gap: 6px;
}

.image-fields label > span,
.image-fields legend {
  color: var(--admin-text);
  font-size: 11px;
  font-weight: 600;
}

.image-fields label > span small {
  margin-left: 5px;
  color: var(--admin-subtle);
  font-weight: 400;
}

.image-fields input[type="text"] {
  width: 100%;
  min-height: 38px;
  padding: 0 10px;
  color: var(--admin-text);
  border: 1px solid var(--admin-border);
  border-radius: 6px;
  background: var(--admin-surface);
  outline: 0;
}

.image-fields input[type="text"]:focus {
  border-color: var(--admin-accent);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--admin-accent) 12%, transparent);
}

.image-fields fieldset {
  min-width: 0;
  margin: 0;
  padding: 0;
  border: 0;
}

.image-fields legend {
  margin-bottom: 7px;
}

.segmented-control {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  overflow: hidden;
  border: 1px solid var(--admin-border);
  border-radius: 6px;
}

.segmented-control.size-control {
  grid-template-columns: repeat(4, 1fr);
}

.segmented-control label {
  min-width: 0;
  position: relative;
  border-right: 1px solid var(--admin-border);
}

.segmented-control label:last-child {
  border-right: 0;
}

.segmented-control input {
  position: absolute;
  opacity: 0;
  pointer-events: none;
}

.segmented-control span {
  min-height: 34px;
  display: grid;
  padding: 0 7px;
  place-items: center;
  color: var(--admin-subtle);
  background: var(--admin-surface);
  font-size: 10px !important;
  font-weight: 500 !important;
  cursor: pointer;
}

.segmented-control input:checked + span {
  color: var(--admin-accent);
  background: color-mix(in srgb, var(--admin-accent) 10%, var(--admin-surface));
  box-shadow: inset 0 -2px var(--admin-accent);
}

.image-panel footer {
  position: sticky;
  bottom: 0;
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 14px 20px;
  border-top: 1px solid var(--admin-divider);
  background: var(--admin-surface);
}

.image-panel footer > span {
  flex: 1;
}

.image-panel footer button {
  min-height: 36px;
  padding: 0 15px;
  color: var(--admin-text);
  border: 1px solid var(--admin-border);
  border-radius: 6px;
  background: var(--admin-surface);
}

.image-panel footer .save-image {
  color: white;
  border-color: var(--admin-accent);
  background: var(--admin-accent);
}

.image-panel footer .remove-image {
  color: #c33e3e;
  border-color: color-mix(in srgb, #c33e3e 32%, var(--admin-border));
}

@media (max-width: 1040px) {
  .editor-canvas {
    grid-template-columns: 1fr;
  }

  .source-pane {
    border-right: 0;
    border-bottom: 1px solid var(--admin-border);
  }

  .preview-pane {
    min-height: 460px;
  }

  .ribbon-heading-actions > span {
    display: none;
  }
}

@media (max-width: 720px) {
  .ribbon-tools {
    align-items: stretch;
  }

  .ribbon-tabs span,
  .status-tip {
    display: none;
  }

  .image-panel-backdrop {
    padding: 0;
  }

  .image-panel {
    width: 100%;
    max-height: 100vh;
    border-radius: 0;
  }

  .image-panel-body {
    grid-template-columns: 1fr;
    padding: 16px;
  }

  .image-stage {
    min-height: 210px;
  }

  .source-pane textarea,
  .preview-pane > article {
    padding-inline: 18px;
  }
}
</style>
