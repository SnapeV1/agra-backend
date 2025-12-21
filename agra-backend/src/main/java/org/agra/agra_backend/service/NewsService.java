package org.agra.agra_backend.service;

import org.agra.agra_backend.dao.NewsArticleRepository;
import org.agra.agra_backend.model.AdminSettings;
import org.agra.agra_backend.model.NewsArticle;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class NewsService {

    private final NewsArticleRepository repository;
    private final AdminSettingsService adminSettingsService;
    private final RestTemplate restTemplate = new RestTemplate();
    private static final Logger log = LoggerFactory.getLogger(NewsService.class);

    @Value("${gnews.api.key:YOUR_API_KEY}")
    private String apiKey;

    @Value("${gnews.base-url:https://gnews.io/api/v4/search}")
    private String baseUrl;

    public NewsService(NewsArticleRepository repository, AdminSettingsService adminSettingsService) {
        this.repository = repository;
        this.adminSettingsService = adminSettingsService;
    }

    @CacheEvict(cacheNames = {"news:list", "news:latest"}, allEntries = true)
    public void fetchWeeklyNews() {
        String[] countries = {"tn", "dz", "ma", "eg", "ly", "mr"};
        log.info("NewsService.fetchWeeklyNews - start (countries={})", java.util.Arrays.toString(countries));
        for (String country : countries) {
            String url = baseUrl + "?q=agriculture&country=" + country + "&lang=fr&token=" + apiKey;
            log.debug("Fetching GNews for country={} url={}", country, url);
            ResponseEntity<Map<String, Object>> responseEntity = restTemplate().exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            Map<String, Object> response = responseEntity.getBody();
            if (response == null) {
                log.warn("No response body for country={}", country);
                continue;
            }

            List<Map<String, Object>> articles = (List<Map<String, Object>>) response.get("articles");
            if (articles == null) {
                log.warn("No 'articles' field in response for country={}", country);
                continue;
            }
            log.info("Parsed {} articles for country={}", articles.size(), country);

            for (Map<String, Object> article : articles) {
                NewsArticle news = new NewsArticle();
                news.setTitle((String) article.get("title"));
                news.setDescription((String) article.get("description"));
                news.setUrl((String) article.get("url"));
                Object sourceObj = article.get("source");
                if (sourceObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> sourceMap = (Map<String, Object>) sourceObj;
                    news.setSource((String) sourceMap.get("name"));
                }
                news.setPublishedAt((String) article.get("publishedAt"));
                news.setCountry(country);

                repository.save(news);
                log.debug("Saved article title='{}' country={}", news.getTitle(), country);
            }
        }
        log.info("NewsService.fetchWeeklyNews - done");
    }

    /**
     * Performs a minimal request to GNews to check API reachability and token validity.
     * @return true if reachable and returns HTTP 200 with an 'articles' array, false otherwise
     */
    public boolean testGNewsConnectivity() {
        try {
            String url = baseUrl + "?q=agriculture&max=1&lang=fr&token=" + apiKey;
            log.debug("Testing GNews connectivity: url={}", url);
            ResponseEntity<Map<String, Object>> responseEntity = restTemplate().exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            Map<String, Object> body = responseEntity.getBody();
            boolean ok = responseEntity.getStatusCode().is2xxSuccessful()
                    && body != null
                    && body.containsKey("articles");
            log.info("GNews connectivity status: {} (http={}, hasArticles={})",
                    ok,
                    responseEntity.getStatusCode().value(),
                    body != null && body.containsKey("articles"));
            return ok;
        } catch (Exception e) {
            log.error("GNews connectivity test failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Fetch agriculture news for North African countries immediately and return saved articles.
     */
    @Caching(cacheable = {
            @Cacheable(cacheNames = "news:latest", key = "'north-africa'")
    }, evict = {
            @CacheEvict(cacheNames = "news:list", allEntries = true, beforeInvocation = false)
    })
    public java.util.List<NewsArticle> fetchNorthAfricaAgricultureNow() {
        repository.deleteAll();
        String[] countries = {"tn", "dz", "ma", "eg", "ly", "mr"};
        java.util.List<NewsArticle> collected = new java.util.ArrayList<>();
        log.info("NewsService.fetchNorthAfricaAgricultureNow - start");
        for (String country : countries) {
            String url = baseUrl + "?q=agriculture&country=" + country + "&lang=fr&token=" + apiKey;
            log.debug("Fetching NOW for country={} url={}", country, url);
            try {
                ResponseEntity<Map<String, Object>> responseEntity = restTemplate().exchange(
                        url,
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<Map<String, Object>>() {}
                );
                Map<String, Object> response = responseEntity.getBody();
                if (response == null) {
                    log.warn("No response body (NOW) for country={}", country);
                    continue;
                }

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> articles = (List<Map<String, Object>>) response.get("articles");
                if (articles == null) {
                    log.warn("No 'articles' (NOW) for country={}", country);
                    continue;
                }

                for (Map<String, Object> article : articles) {
                    NewsArticle news = new NewsArticle();
                    news.setTitle((String) article.get("title"));
                    news.setDescription((String) article.get("description"));
                    news.setUrl((String) article.get("url"));
                    Object sourceObj = article.get("source");
                    if (sourceObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> sourceMap = (Map<String, Object>) sourceObj;
                        news.setSource((String) sourceMap.get("name"));
                    }
                    news.setPublishedAt((String) article.get("publishedAt"));
                    news.setCountry(country);

                    repository.save(news);
                    collected.add(news);
                }
            } catch (Exception ex) {
                log.error("Error fetching NOW for country={}: {}", country, ex.getMessage());
            }
        }
        log.info("NewsService.fetchNorthAfricaAgricultureNow - done (saved={})", collected.size());
        return collected;
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = {"news:list", "news:latest"}, allEntries = true)
    })
    public void deleteById(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("News id must not be empty");
        }
        boolean exists = repository.existsById(id);
        if (!exists) {
            throw new RuntimeException("News article not found");
        }
        repository.deleteById(id);
        log.info("Deleted news article id={}", id);
    }

    public AdminSettings getAdminSettings() {
        return adminSettingsService.getSettings();
    }

    public AdminSettings updateNewsCron(String cron) {
        return adminSettingsService.updateNewsCron(cron);
    }

    protected RestTemplate restTemplate() {
        return restTemplate;
    }
}
