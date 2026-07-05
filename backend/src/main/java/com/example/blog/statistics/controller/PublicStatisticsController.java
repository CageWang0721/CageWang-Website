package com.example.blog.statistics.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.blog.interaction.service.VisitorContext;
import com.example.blog.statistics.dto.PublicSiteStatistics;
import com.example.blog.statistics.service.SiteStatisticsService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/v1/public/statistics")
public class PublicStatisticsController {

    private final SiteStatisticsService service;
    private final VisitorContext visitors;

    public PublicStatisticsController(
            SiteStatisticsService service,
            VisitorContext visitors
    ) {
        this.service = service;
        this.visitors = visitors;
    }

    @GetMapping
    PublicSiteStatistics statistics(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        return service.snapshot(visitors.resolve(request, response));
    }
}
