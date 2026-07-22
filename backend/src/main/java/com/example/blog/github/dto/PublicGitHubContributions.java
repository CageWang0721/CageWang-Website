package com.example.blog.github.dto;

import java.time.Instant;
import java.util.List;

public record PublicGitHubContributions(
        String username,
        int totalContributions,
        List<GitHubContributionDay> days,
        Instant syncedAt
) {
}
