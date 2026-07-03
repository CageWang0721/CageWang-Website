package com.example.blog.statistics.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
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

import com.example.blog.interaction.service.VisitorContext.Visitor;
import com.example.blog.shared.cache.PublicContentCache;
import com.example.blog.shared.config.RuntimeProperties;
import com.example.blog.statistics.mapper.ViewStatisticsMapper;

@ExtendWith(MockitoExtension.class)
class ViewCounterServiceTests {

    @Mock private StringRedisTemplate redis;
    @Mock private ValueOperations<String, String> values;
    @Mock private SetOperations<String, String> sets;
    @Mock private ViewStatisticsMapper mapper;
    @Mock private PublicContentCache cache;

    private ViewCounterService service;

    @BeforeEach
    void setUp() {
        when(redis.opsForValue()).thenReturn(values);
        when(redis.opsForSet()).thenReturn(sets);
        RuntimeProperties runtime = new RuntimeProperties(
                "blog", "test", true,
                Duration.ofMinutes(10),
                Duration.ofMinutes(5),
                Duration.ofMinutes(30)
        );
        service = new ViewCounterService(redis, mapper, runtime, cache);
    }

    @Test
    void recordsPageAndUniqueCountersWithoutRawIdentity() {
        when(mapper.articleIsPublic(7L)).thenReturn(true);
        when(values.increment(anyString())).thenReturn(1L);
        when(redis.expire(anyString(), any(Duration.class))).thenReturn(true);

        var result = service.record(
                7L,
                new Visitor("a".repeat(64), "b".repeat(64), "test"),
                "https://www.google.com/search?q=blog"
        );

        assertTrue(result.accepted());
        verify(values, atLeastOnce()).increment(anyString());
        verify(sets, atLeastOnce()).add(anyString(), anyString());
    }
}
