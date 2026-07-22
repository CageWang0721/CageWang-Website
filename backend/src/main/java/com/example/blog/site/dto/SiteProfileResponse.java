package com.example.blog.site.dto;

public record SiteProfileResponse(
        String avatarUrl,
        String signature,
        boolean musicEnabled,
        String musicTitle,
        String musicArtist,
        String musicUrl,
        String musicCoverUrl
) {
}
