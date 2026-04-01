package com.koval.githubreposcorer.cache;

import com.koval.githubreposcorer.model.github.RepositoryItemResponse;
import com.koval.githubreposcorer.service.GithubSearchService;
import com.koval.githubreposcorer.service.PopularRepositoryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@Testcontainers
class PopularRepositoryCacheIT {

    @Container
    @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @MockitoBean
    GithubSearchService githubSearchService;

    @Autowired
    PopularRepositoryService service;

    @Autowired
    CacheManager cacheManager;

    @AfterEach
    void clearCache() {
        cacheManager.getCache("popularRepositories").clear();
    }

    @Test
    void firstCall_hitsService_secondCall_returnsFromCache() {
        var language = "Java";
        var createdAfter = LocalDate.of(2024, 1, 1);
        var repo = repo(1L, "owner/repo", 1000, 200);

        when(githubSearchService.fetchTopStarred(language, createdAfter)).thenReturn(List.of(repo));
        when(githubSearchService.fetchTopForked(language, createdAfter)).thenReturn(List.of());

        // first call — must hit the service
        var first = service.getPopularRepos(language, createdAfter);

        // second call — identical params, must come from cache
        var second = service.getPopularRepos(language, createdAfter);

        // service invoked exactly once across both calls
        verify(githubSearchService, times(1)).fetchTopStarred(language, createdAfter);
        verify(githubSearchService, times(1)).fetchTopForked(language, createdAfter);

        // cached response deserializes back to the correct type and values
        assertNotNull(second.items());
        assertEquals(1, second.items().size());
        assertEquals(1L, second.items().getFirst().id());
        assertEquals("owner/repo", second.items().getFirst().fullName());
        assertEquals(1000, second.items().getFirst().stars());
        assertEquals(200, second.items().getFirst().forks());

        // scores are identical (not re-computed)
        assertEquals(first.items().getFirst().score(), second.items().getFirst().score());
    }

    private static RepositoryItemResponse repo(long id, String name, int stars, int forks) {
        Instant now = Instant.now().minus(1, ChronoUnit.DAYS);
        return new RepositoryItemResponse(id, name, "https://github.com/" + name,
                "Java", stars, forks, now, now);
    }
}
