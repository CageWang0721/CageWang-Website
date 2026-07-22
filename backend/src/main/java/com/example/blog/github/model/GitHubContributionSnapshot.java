package com.example.blog.github.model;

import java.time.LocalDateTime;

public record GitHubContributionSnapshot(
        String username,
        int totalContributions,
        String contributionsJson,
        LocalDateTime syncedAt
) {
}
