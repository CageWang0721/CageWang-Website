import type { SiteStatistics } from '~/types/blog'

export function useSiteStatistics() {
  return useState<SiteStatistics>('site-statistics', () => ({
    onlineVisitors: 0,
    todayViews: 0,
    totalViews: 0,
    totalVisitors: 0
  }))
}
