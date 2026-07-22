package com.example.blog.site.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SiteProfileUpdateRequest(
        @Size(max = 1000) String avatarUrl,
        @NotBlank @Size(max = 160) String signature,
        boolean musicEnabled,
        @Size(max = 120) String musicTitle,
        @Size(max = 120) String musicArtist,
        @Size(max = 1000) String musicUrl,
        @Size(max = 1000) String musicCoverUrl
) {
}
