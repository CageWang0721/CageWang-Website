package com.example.blog.media.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.blog.media.config.MediaLifecycleProperties;
import com.example.blog.media.mapper.MediaMapper;
import com.example.blog.media.model.MediaAssetRecord;
import com.example.blog.media.storage.ObjectStorage;

@ExtendWith(MockitoExtension.class)
class MediaLifecycleProcessorTests {

    @Mock
    private MediaMapper mediaMapper;

    @Mock
    private MediaService mediaService;

    @Mock
    private ObjectStorage objectStorage;

    private MediaLifecycleProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new MediaLifecycleProcessor(
                mediaMapper,
                mediaService,
                objectStorage,
                new MediaLifecycleProperties(
                        Duration.ofHours(24),
                        Duration.ofMinutes(1),
                        Duration.ofMinutes(15),
                        50
                )
        );
    }

    @Test
    void reconcilesReferencesAndProcessesCleanupCandidates() {
        MediaAssetRecord candidate = candidate();
        when(objectStorage.configured()).thenReturn(true);
        when(mediaMapper.findDeleteCandidates(any(), eq(50)))
                .thenReturn(List.of(candidate));

        processor.reconcileAndCleanup();

        verify(mediaMapper).reconcileReferences(any(LocalDateTime.class));
        verify(mediaMapper).markOrphans(any(LocalDateTime.class));
        verify(mediaService).processDeleteCandidate(eq(candidate), any(LocalDateTime.class));
    }

    @Test
    void skipsCleanupWhenOssIsNotConfigured() {
        when(objectStorage.configured()).thenReturn(false);

        processor.reconcileAndCleanup();

        verify(mediaMapper, never()).reconcileReferences(any());
    }

    private MediaAssetRecord candidate() {
        return new MediaAssetRecord(
                9L,
                "blog/test/2026/07/image.png",
                "cover.png",
                "image/png",
                "png",
                100,
                24,
                12,
                "0".repeat(64),
                "Cover",
                "ORPHAN",
                0,
                LocalDateTime.of(2026, 7, 1, 8, 0),
                0,
                null,
                null,
                1L,
                LocalDateTime.of(2026, 7, 1, 8, 0),
                null
        );
    }
}
