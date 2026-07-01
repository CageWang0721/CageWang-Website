package com.example.blog.article.controller;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.blog.article.dto.PublicArchiveMonth;
import com.example.blog.article.dto.PublicArticleCard;
import com.example.blog.article.dto.PublicArticleDetail;
import com.example.blog.article.dto.PublicArticleNavigation;
import com.example.blog.article.dto.PublicArticlePage;
import com.example.blog.article.dto.PublicHomeResponse;
import com.example.blog.article.dto.PublicTaxonomyItem;
import com.example.blog.article.service.PublicArticleService;

@RestController
@RequestMapping("/api/v1/public")
public class PublicArticleController {

    private final PublicArticleService service;

    public PublicArticleController(PublicArticleService service) {
        this.service = service;
    }

    @GetMapping("/home")
    PublicHomeResponse home() {
        return service.home();
    }

    @GetMapping("/articles")
    PublicArticlePage articles(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int pageSize
    ) {
        return service.findArticles(keyword, category, tag, page, pageSize);
    }

    @GetMapping("/articles/{slug}")
    ResponseEntity<PublicArticleDetail> article(@PathVariable String slug) {
        try {
            return ResponseEntity.ok(service.findBySlug(slug));
        } catch (com.example.blog.shared.error.ApiException exception) {
            Optional<String> redirect = service.findRedirect(slug);
            if (redirect.isPresent()) {
                return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
                        .location(URI.create(redirect.get()))
                        .build();
            }
            throw exception;
        }
    }

    @GetMapping("/articles/{slug}/adjacent")
    PublicArticleNavigation adjacent(@PathVariable String slug) {
        return service.adjacent(slug);
    }

    @GetMapping("/articles/{slug}/related")
    List<PublicArticleCard> related(@PathVariable String slug) {
        return service.related(slug);
    }

    @GetMapping("/categories")
    List<PublicTaxonomyItem> categories() {
        return service.categories();
    }

    @GetMapping("/categories/{slug}/articles")
    PublicArticlePage categoryArticles(
            @PathVariable String slug,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int pageSize
    ) {
        return service.findArticles(null, slug, null, page, pageSize);
    }

    @GetMapping("/tags")
    List<PublicTaxonomyItem> tags() {
        return service.tags();
    }

    @GetMapping("/tags/{slug}/articles")
    PublicArticlePage tagArticles(
            @PathVariable String slug,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int pageSize
    ) {
        return service.findArticles(null, null, slug, page, pageSize);
    }

    @GetMapping("/archives")
    List<PublicArchiveMonth> archives() {
        return service.archives();
    }

    @GetMapping("/search")
    PublicArticlePage search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int pageSize
    ) {
        return service.findArticles(keyword, null, null, page, pageSize);
    }
}
