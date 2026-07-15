import type { RepeatMode } from '~/types/music'

export function formatDuration(value: number) {
  if (!Number.isFinite(value) || value <= 0) return '0:00'
  const seconds = Math.floor(value)
  return `${Math.floor(seconds / 60)}:${String(seconds % 60).padStart(2, '0')}`
}

export function getNextIndex(current: number, length: number, repeat: RepeatMode) {
  if (length <= 0 || current < 0) return null
  if (repeat === 'one') return current
  if (current < length - 1) return current + 1
  return repeat === 'all' ? 0 : null
}

export function getPreviousIndex(current: number, length: number) {
  if (length <= 0 || current < 0) return null
  return current > 0 ? current - 1 : length - 1
}
