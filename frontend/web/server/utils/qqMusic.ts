import Meting from '@meting/core'

export const QQ_PLAYLIST_ID = '9747059163'

const PLAYLIST_CACHE_TTL = 10 * 60 * 1000
const COVER_PLACEHOLDER = '/images/wineclouds-avatar.png'

interface RawMetingTrack {
  id?: unknown
  name?: unknown
  artist?: unknown
  pic_id?: unknown
  url_id?: unknown
}

interface CachedTrack {
  id: string
  title: string
  artist: string
  picId: string
  urlId: string
}

export interface MusicTrack {
  id: string
  title: string
  artist: string
  cover: string
}

interface PlaylistCache {
  expiresAt: number
  tracks: CachedTrack[]
  items: MusicTrack[]
}

let cache: PlaylistCache | null = null

const meting = new Meting('tencent').format(true)

function asNonEmptyString(value: unknown) {
  return typeof value === 'string' && value.trim() ? value.trim() : null
}

function normalizeTrack(value: unknown): CachedTrack | null {
  if (!value || typeof value !== 'object') return null

  const track = value as RawMetingTrack
  const id = asNonEmptyString(track.id)
  const title = asNonEmptyString(track.name)
  const picId = asNonEmptyString(track.pic_id)
  const urlId = asNonEmptyString(track.url_id)
  const artist = Array.isArray(track.artist)
    ? track.artist.map(asNonEmptyString).filter((name): name is string => Boolean(name)).join(' / ')
    : ''

  if (!id || !title || !artist || !picId || !urlId) return null

  return { id, title, artist, picId, urlId }
}

async function mapWithConcurrency<T, R>(items: T[], limit: number, mapper: (item: T) => Promise<R>) {
  const results = new Array<R>(items.length)
  let nextIndex = 0

  await Promise.all(Array.from({ length: Math.min(limit, items.length) }, async () => {
    while (nextIndex < items.length) {
      const currentIndex = nextIndex++
      results[currentIndex] = await mapper(items[currentIndex])
    }
  }))

  return results
}

async function resolveCover(picId: string) {
  try {
    const result = JSON.parse(await meting.pic(picId, 300)) as { url?: unknown }
    return asNonEmptyString(result.url) ?? COVER_PLACEHOLDER
  } catch {
    return COVER_PLACEHOLDER
  }
}

async function refreshPlaylist() {
  const parsed = JSON.parse(await meting.playlist(QQ_PLAYLIST_ID)) as unknown
  if (!Array.isArray(parsed)) throw new Error('QQ music playlist response is invalid')

  const tracks = parsed.map(normalizeTrack).filter((track): track is CachedTrack => Boolean(track))
  if (!tracks.length) throw new Error('QQ music playlist has no playable tracks')

  const items = await mapWithConcurrency(tracks, 4, async (track) => ({
    id: track.id,
    title: track.title,
    artist: track.artist,
    cover: await resolveCover(track.picId)
  }))

  cache = {
    expiresAt: Date.now() + PLAYLIST_CACHE_TTL,
    tracks,
    items
  }

  return cache
}

async function getCache() {
  if (cache && cache.expiresAt > Date.now()) return cache
  return refreshPlaylist()
}

export async function getPlaylist() {
  return (await getCache()).items
}

export async function getTrackStream(trackId: string) {
  const track = (await getCache()).tracks.find((item) => item.id === trackId)
  if (!track) return null

  try {
    const response = JSON.parse(await meting.url(track.urlId, 320)) as { url?: unknown }
    const url = asNonEmptyString(response.url)
    return url && /^https?:\/\//.test(url) ? { url } : null
  } catch {
    return null
  }
}
