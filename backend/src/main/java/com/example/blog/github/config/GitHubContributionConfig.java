package com.example.blog.github.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GitHubContributionProperties.class)
public class GitHubContributionConfig {
}
