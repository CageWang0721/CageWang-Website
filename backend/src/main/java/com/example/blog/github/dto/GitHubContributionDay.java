package com.example.blog.github.dto;

import java.time.LocalDate;

public record GitHubContributionDay(
        LocalDate date,
        int contributionCount
) {
}
