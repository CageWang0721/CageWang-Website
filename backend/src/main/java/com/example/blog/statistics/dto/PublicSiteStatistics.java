package com.example.blog.statistics.dto;

public record PublicSiteStatistics(
        long onlineVisitors,
        long todayViews,
        long totalViews,
        long totalVisitors
) {
}
