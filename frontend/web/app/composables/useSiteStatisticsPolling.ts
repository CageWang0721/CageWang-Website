const REFRESH_INTERVAL_MS = 60_000

export function useSiteStatisticsPolling() {
  const api = useBlogApi()
  const siteStatistics = useSiteStatistics()
  let refreshTimer: number | undefined

  const refreshStatistics = () => {
    if (document.visibilityState !== 'visible') return

    void api.statistics()
      .then((statistics) => {
        siteStatistics.value = statistics
      })
      .catch(() => undefined)
  }

  onMounted(() => {
    refreshStatistics()
    refreshTimer = window.setInterval(refreshStatistics, REFRESH_INTERVAL_MS)
    window.addEventListener('focus', refreshStatistics)
  })

  onBeforeUnmount(() => {
    if (refreshTimer !== undefined) window.clearInterval(refreshTimer)
    window.removeEventListener('focus', refreshStatistics)
  })
}
