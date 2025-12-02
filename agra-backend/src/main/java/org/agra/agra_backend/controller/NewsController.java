package org.agra.agra_backend.controller;

import org.agra.agra_backend.dao.NewsArticleRepository;
import org.agra.agra_backend.model.NewsArticle;
import org.agra.agra_backend.service.NewsService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/news")
public class NewsController {

    private final NewsService newsService;
    private final NewsArticleRepository repository;
    private static final Logger log = LoggerFactory.getLogger(NewsController.class);

    public NewsController(NewsService newsService, NewsArticleRepository repository) {
        this.newsService = newsService;
        this.repository = repository;
    }

    @PostMapping("/fetch-weekly")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> fetchWeeklyNews() {
        try {
            log.info("POST /api/news/fetch-weekly - start");
            newsService.fetchWeeklyNews();
            log.info("POST /api/news/fetch-weekly - success");
            return ResponseEntity.ok().body("Weekly news fetched and stored successfully.");
        } catch (Exception e) {
            log.error("POST /api/news/fetch-weekly - error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to fetch weekly news: " + e.getMessage());
        }
    }

    @GetMapping("/fetch-now")
    public ResponseEntity<Map<String, Object>> fetchNow() {
        log.info("GET /api/news/fetch-now - start");
        var articles = newsService.fetchNorthAfricaAgricultureNow();
        Map<String, Object> resp = new HashMap<>();
        resp.put("count", articles.size());
        resp.put("articles", articles);
        log.info("GET /api/news/fetch-now - done (count={})", articles.size());
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        boolean gnewsOk = newsService.testGNewsConnectivity();
        boolean mongoOk;
        try {
            repository.count();
            mongoOk = true;
        } catch (Exception e) {
            mongoOk = false;
            log.error("Mongo connectivity test failed: {}", e.getMessage());
        }

        Map<String, Object> status = new HashMap<>();
        status.put("status", (gnewsOk && mongoOk) ? "ok" : "degraded");
        status.put("gnews", gnewsOk);
        status.put("mongo", mongoOk);
        return ResponseEntity.ok(status);
    }

    @GetMapping("/getnews")
    @Cacheable(cacheNames = "news:list", key = "T(java.util.Objects).toString(#country) + '|' + T(java.util.Objects).toString(#date)")
    public java.util.List<NewsArticle> getAllNews(
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String date
    ) {
        java.util.List<NewsArticle> result;
        if (country != null && date != null) {
            result = repository.findByCountryIgnoreCaseAndPublishedAtStartingWith(country, date);
        } else if (country != null) {
            result = repository.findByCountryIgnoreCase(country);
        } else if (date != null) {
            result = repository.findByPublishedAtStartingWith(date);
        } else {
            result = repository.findAll();
        }
        log.debug("GET /api/news/getnews - params country={}, date={}, resultCount={}", country, date, result.size());
        return result;
    }

    @GetMapping("/all")
    @Cacheable(cacheNames = "news:list", key = "'all'")
    public java.util.List<NewsArticle> getAll() {
        var list = repository.findAll();
        log.debug("GET /api/news/all - resultCount={}", list.size());
        return list;
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deleteNews(@PathVariable String id) {
        try {
            newsService.deleteById(id);
            return ResponseEntity.ok().body("News article deleted successfully.");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            log.error("DELETE /api/news/{} failed: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to delete news article: " + e.getMessage());
        }
    }
}
