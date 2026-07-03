package com.example.blog.shared.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.example.blog.shared.config.RuntimeProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class PublicContentCacheTests {

    @Mock private StringRedisTemplate redis;
    @Mock private ValueOperations<String, String> values;

    private PublicContentCache cache;
    private final AtomicReference<String> stored = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        RuntimeProperties runtime = new RuntimeProperties(
                "blog", "test", true,
                Duration.ofMinutes(10),
                Duration.ofMinutes(5),
                Duration.ofMinutes(30)
        );
        when(redis.opsForValue()).thenReturn(values);
        when(values.get(anyString())).thenAnswer(ignored -> stored.get());
        org.mockito.Mockito.doAnswer(invocation -> {
            stored.set(invocation.getArgument(1));
            return null;
        }).when(values).set(anyString(), anyString(), any(Duration.class));
        cache = new PublicContentCache(
                redis,
                new ObjectMapper().findAndRegisterModules(),
                runtime,
                new CacheMetrics()
        );
    }

    @Test
    void servesSerializedValueAfterFirstLoad() {
        AtomicInteger loads = new AtomicInteger();

        Sample first = cache.get(
                "sample",
                Duration.ofMinutes(1),
                Sample.class,
                () -> new Sample("value-" + loads.incrementAndGet())
        );
        Sample second = cache.get(
                "sample",
                Duration.ofMinutes(1),
                Sample.class,
                () -> new Sample("value-" + loads.incrementAndGet())
        );

        assertEquals("value-1", first.value());
        assertEquals(first, second);
        assertEquals(1, loads.get());
    }

    private record Sample(String value) {
    }
}
