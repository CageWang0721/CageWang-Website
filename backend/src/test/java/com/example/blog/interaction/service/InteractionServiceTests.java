package com.example.blog.interaction.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.blog.interaction.dto.InteractionState;
import com.example.blog.interaction.mapper.InteractionMapper;
import com.example.blog.shared.error.ApiException;

@ExtendWith(MockitoExtension.class)
class InteractionServiceTests {

    @Mock private InteractionMapper mapper;
    @Mock private PublicInteractionGuard guard;

    private InteractionService service;

    @BeforeEach
    void setUp() {
        service = new InteractionService(mapper, guard);
    }

    @Test
    void likesAreIdempotentPerVisitor() {
        when(mapper.articleIsPublic(7L)).thenReturn(true);
        when(mapper.findState(7L, "visitor")).thenReturn(new InteractionState(1, 0, true));

        var state = service.like(7L, "visitor");

        assertTrue(state.liked());
        verify(mapper).insertLike(7L, "visitor");
        verify(mapper).refreshLikeCount(7L);
    }

    @Test
    void rejectsLikesForNonPublicArticle() {
        when(mapper.articleIsPublic(7L)).thenReturn(false);
        assertThrows(ApiException.class, () -> service.like(7L, "visitor"));
    }
}
