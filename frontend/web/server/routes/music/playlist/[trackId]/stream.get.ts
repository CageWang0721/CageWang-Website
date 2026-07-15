import { getTrackStream } from '../../../../utils/qqMusic'

export default defineEventHandler(async (event) => {
  const trackId = getRouterParam(event, 'trackId')
  const stream = trackId ? await getTrackStream(trackId) : null

  if (!stream) {
    throw createError({
      statusCode: 404,
      statusMessage: 'Track is unavailable'
    })
  }

  return stream
})
