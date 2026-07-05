package com.example.blog.statistics.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

import com.example.blog.interaction.service.VisitorContext.Visitor;
import com.example.blog.shared.config.RuntimeProperties;
import com.example.blog.statistics.mapper.ViewStatisticsMapper;

@ExtendWith(MockitoExtension.class)
class SiteStatisticsServiceTests {

    @Mock private StringRedisTemplate redis;
    @Mock private ValueOperations<String, String> values;
    @Mock private SetOperations<String, String> sets;
    @Mock private ZSetOperations<String, String> sortedSets;
    @Mock private ViewStatisticsMapper mapper;

    private SiteStatisticsService service;

    @BeforeEach
    void setUp() {
        when(redis.opsForValue()).thenReturn(values);
        when(redis.opsForSet()).thenReturn(sets);
        when(redis.opsForZSet()).thenReturn(sortedSets);
        RuntimeProperties runtime = new RuntimeProperties(
                "blog", "test", true,
                Duration.ofMinutes(10),
                Duration.ofMinutes(5),
                Duration.ofMinutes(30)
        );
        service = new SiteStatisticsService(redis, mapper, runtime);
    }

    @Test
    void combinesPersistedPendingAndOnlineStatistics() {
        when(mapper.findSiteViews(any())).thenReturn(5L);
        when(mapper.findSiteVisitors(any())).thenReturn(2L);
        when(mapper.sumSiteViewsBefore(any())).thenReturn(100L);
        when(mapper.sumSiteVisitorsBefore(any())).thenReturn(40L);
        when(values.get(anyString())).thenReturn("2");
        when(sets.size(anyString())).thenReturn(3L);
        when(sortedSets.zCard(anyString())).thenReturn(1L);

        var statistics = service.snapshot(
                new Visitor("a".repeat(64), "b".repeat(64), "test")
        );

        assertEquals(1, statistics.onlineVisitors());
        assertEquals(7, statistics.todayViews());
        assertEquals(107, statistics.totalViews());
        assertEquals(43, statistics.totalVisitors());
        verify(sortedSets).add(anyString(), anyString(), any(Double.class));
    }
}
