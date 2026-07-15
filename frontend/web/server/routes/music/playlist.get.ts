import { getPlaylist } from '../../utils/qqMusic'

export default defineEventHandler(async () => {
  try {
    return { items: await getPlaylist() }
  } catch {
    throw createError({
      statusCode: 502,
      statusMessage: 'QQ music playlist is unavailable'
    })
  }
})
