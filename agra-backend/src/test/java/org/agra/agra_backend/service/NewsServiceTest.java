package org.agra.agra_backend.service;

import org.agra.agra_backend.dao.NewsArticleRepository;
import org.agra.agra_backend.model.AdminSettings;
import org.agra.agra_backend.model.NewsArticle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NewsServiceTest {

    @Mock
    private NewsArticleRepository repository;

    @Mock
    private AdminSettingsService adminSettingsService;

    @Mock
    private RestTemplate restTemplate;

    private NewsService service;

    @BeforeEach
    void setUp() {
        service = new NewsService(repository, adminSettingsService) {
            @Override
            protected RestTemplate restTemplate() {
                return restTemplate;
            }
        };
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        ReflectionTestUtils.setField(service, "baseUrl", "http://example.test");
    }

    @Test
    void fetchWeeklyNewsSkipsNullAndMissingArticles() {
        Map<String, Object> article = Map.of(
                "title", "Agro Update",
                "description", "Weekly update",
                "url", "https://example.test/article",
                "source", Map.of("name", "GNews"),
                "publishedAt", "2025-01-01T10:00:00Z"
        );

        ResponseEntity<Map<String, Object>> nullBody = ResponseEntity.ok().body(null);
        ResponseEntity<Map<String, Object>> noArticles = ResponseEntity.ok(Map.of("totalArticles", 1));
        ResponseEntity<Map<String, Object>> withArticles = ResponseEntity.ok(Map.of("articles", List.of(article)));

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(nullBody, noArticles, withArticles, withArticles, withArticles, withArticles);

        service.fetchWeeklyNews();

        ArgumentCaptor<NewsArticle> captor = ArgumentCaptor.forClass(NewsArticle.class);
        verify(repository, times(4)).save(captor.capture());
        assertThat(captor.getAllValues())
                .allMatch(saved -> "GNews".equals(saved.getSource()))
                .allMatch(saved -> saved.getTitle() != null)
                .allMatch(saved -> saved.getCountry() != null);
    }

    @Test
    void fetchNorthAfricaAgricultureNowCollectsArticlesAndIgnoresErrors() {
        Map<String, Object> article = Map.of(
                "title", "Now Update",
                "description", "Immediate update",
                "url", "https://example.test/now",
                "source", Map.of("name", "NowSource"),
                "publishedAt", "2025-01-02T10:00:00Z"
        );

        ResponseEntity<Map<String, Object>> nullBody = ResponseEntity.ok().body(null);
        ResponseEntity<Map<String, Object>> noArticles = ResponseEntity.ok(Map.of("totalArticles", 1));
        ResponseEntity<Map<String, Object>> withArticles = ResponseEntity.ok(Map.of("articles", List.of(article)));

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RuntimeException("boom"))
                .thenReturn(nullBody, noArticles, withArticles, withArticles, withArticles);

        List<NewsArticle> saved = service.fetchNorthAfricaAgricultureNow();

        verify(repository).deleteAll();
        verify(repository, times(3)).save(any(NewsArticle.class));
        assertThat(saved).hasSize(3);
        Set<String> countries = saved.stream().map(NewsArticle::getCountry).collect(Collectors.toSet());
        assertThat(countries).containsExactlyInAnyOrder("eg", "ly", "mr");
    }

    @Test
    void testGNewsConnectivityReturnsTrueWhenOk() {
        ResponseEntity<Map<String, Object>> response = ResponseEntity.ok(Map.of("articles", List.of()));
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(response);

        assertThat(service.testGNewsConnectivity()).isTrue();
    }

    @Test
    void testGNewsConnectivityReturnsFalseWhenNoArticles() {
        ResponseEntity<Map<String, Object>> response = ResponseEntity.ok(Map.of("status", "ok"));
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(response);

        assertThat(service.testGNewsConnectivity()).isFalse();
    }

    @Test
    void testGNewsConnectivityReturnsFalseOnException() {
        doThrow(new RuntimeException("down")).when(restTemplate).exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                any(ParameterizedTypeReference.class)
        );

        assertThat(service.testGNewsConnectivity()).isFalse();
    }

    @Test
    void deleteByIdThrowsOnBlank() {
        assertThatThrownBy(() -> service.deleteById("  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be empty");
    }

    @Test
    void deleteByIdThrowsWhenMissing() {
        when(repository.existsById("n1")).thenReturn(false);

        assertThatThrownBy(() -> service.deleteById("n1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void deleteByIdDeletesWhenPresent() {
        when(repository.existsById("n2")).thenReturn(true);

        service.deleteById("n2");

        verify(repository).deleteById("n2");
    }

    @Test
    void getAdminSettingsDelegates() {
        AdminSettings settings = new AdminSettings("settings");
        when(adminSettingsService.getSettings()).thenReturn(settings);

        assertThat(service.getAdminSettings()).isSameAs(settings);
    }

    @Test
    void updateNewsCronDelegates() {
        AdminSettings settings = new AdminSettings("settings");
        when(adminSettingsService.updateNewsCron("0 0 9 ? * MON")).thenReturn(settings);

        assertThat(service.updateNewsCron("0 0 9 ? * MON")).isSameAs(settings);
    }
}
