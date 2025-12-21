package org.agra.agra_backend.controller;

import org.agra.agra_backend.dao.NewsArticleRepository;
import org.agra.agra_backend.model.NewsArticle;
import org.agra.agra_backend.service.NewsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NewsControllerTest {

    @Mock
    private NewsService newsService;

    @Mock
    private NewsArticleRepository repository;

    @InjectMocks
    private NewsController controller;

    @Test
    void fetchWeeklyNewsReturnsOkOnSuccess() {
        doNothing().when(newsService).fetchWeeklyNews();

        ResponseEntity<?> response = controller.fetchWeeklyNews();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(newsService).fetchWeeklyNews();
    }

    @Test
    void fetchNowReturnsArticles() {
        when(newsService.fetchNorthAfricaAgricultureNow()).thenReturn(List.of(new NewsArticle()));

        ResponseEntity<Map<String, Object>> response = controller.fetchNow();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("count", 1);
    }

    @Test
    void pingReportsOkWhenDependenciesHealthy() {
        when(newsService.testGNewsConnectivity()).thenReturn(true);
        when(repository.count()).thenReturn(1L);

        ResponseEntity<Map<String, Object>> response = controller.ping();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("status", "ok");
    }

    @Test
    void pingReportsDegradedWhenMongoFails() {
        when(newsService.testGNewsConnectivity()).thenReturn(true);
        doThrow(new RuntimeException("mongo down")).when(repository).count();

        ResponseEntity<Map<String, Object>> response = controller.ping();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("status", "degraded");
    }

    @Test
    void getAllNewsSupportsCountryAndDateFilters() {
        when(repository.findByCountryIgnoreCaseAndPublishedAtStartingWith("gh", "2025-01-01"))
                .thenReturn(List.of(new NewsArticle()));

        List<NewsArticle> result = controller.getAllNews("gh", "2025-01-01");

        assertThat(result).hasSize(1);
    }

    @Test
    void getAllNewsSupportsCountryOnly() {
        when(repository.findByCountryIgnoreCase("ke")).thenReturn(List.of(new NewsArticle()));

        List<NewsArticle> result = controller.getAllNews("ke", null);

        assertThat(result).hasSize(1);
    }

    @Test
    void getAllNewsSupportsDateOnly() {
        when(repository.findByPublishedAtStartingWith("2025-01-01")).thenReturn(List.of(new NewsArticle()));

        List<NewsArticle> result = controller.getAllNews(null, "2025-01-01");

        assertThat(result).hasSize(1);
    }

    @Test
    void getAllNewsReturnsAllWhenNoFilters() {
        when(repository.findAll()).thenReturn(List.of(new NewsArticle(), new NewsArticle()));

        List<NewsArticle> result = controller.getAllNews(null, null);

        assertThat(result).hasSize(2);
    }

    @Test
    void deleteNewsReturnsNotFoundWhenMissing() {
        doThrow(new RuntimeException("missing")).when(newsService).deleteById("n1");

        ResponseEntity<?> response = controller.deleteNews("n1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
