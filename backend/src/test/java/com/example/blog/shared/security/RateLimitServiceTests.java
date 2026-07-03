package com.example.blog.shared.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.example.blog.shared.config.RuntimeProperties;
import com.example.blog.shared.error.ApiException;

@ExtendWith(MockitoExtension.class)
class RateLimitServiceTests {

    @Mock private StringRedisTemplate redis;
    @Mock private ValueOperations<String, String> values;

    private RateLimitService service;

    @BeforeEach
    void setUp() {
        RuntimeProperties runtime = new RuntimeProperties(
                "blog", "test", true,
                Duration.ofMinutes(10),
                Duration.ofMinutes(5),
                Duration.ofMinutes(30)
        );
        when(redis.opsForValue()).thenReturn(values);
        service = new RateLimitService(
                redis,
                runtime,
                new SecretKeySpec(
                        "test-secret-at-least-thirty-two-bytes".getBytes(StandardCharsets.UTF_8),
                        "HmacSHA256"
                )
        );
    }

    @Test
    void rejectsRequestsBeyondWindowLimit() {
        when(values.increment(anyString())).thenReturn(1L, 2L);
        when(redis.expire(anyString(), any(Duration.class))).thenReturn(true);

        assertDoesNotThrow(() -> service.enforce("search", "subject", 1, Duration.ofMinutes(1)));
        assertThrows(
                ApiException.class,
                () -> service.enforce("search", "subject", 1, Duration.ofMinutes(1))
        );
    }
}
