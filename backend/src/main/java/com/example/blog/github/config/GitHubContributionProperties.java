package com.example.blog.github.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("blog.github-contributions")
public record GitHubContributionProperties(
        boolean enabled,
        String username,
        String token,
        String cron,
        String zone
) {
    public boolean readyToSync() {
        return enabled && username != null && !username.isBlank() && token != null && !token.isBlank();
    }
}
