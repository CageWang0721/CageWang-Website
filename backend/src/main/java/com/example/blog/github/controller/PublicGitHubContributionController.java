package com.example.blog.github.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.blog.github.dto.PublicGitHubContributions;
import com.example.blog.github.service.GitHubContributionService;

@RestController
@RequestMapping("/api/v1/public/github-contributions")
public class PublicGitHubContributionController {

    private final GitHubContributionService service;

    public PublicGitHubContributionController(GitHubContributionService service) {
        this.service = service;
    }

    @GetMapping
    PublicGitHubContributions contributions() {
        return service.publicSnapshot();
    }
}
