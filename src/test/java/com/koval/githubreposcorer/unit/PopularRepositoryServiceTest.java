package com.koval.githubreposcorer.unit;

import com.koval.githubreposcorer.api.response.PopularRepositoryResponse;
import com.koval.githubreposcorer.model.domain.SupportedLanguage;
import com.koval.githubreposcorer.model.github.RepositoryItemResponse;
import com.koval.githubreposcorer.model.result.ScoredRepository;
import com.koval.githubreposcorer.service.GithubSearchService;
import com.koval.githubreposcorer.service.PopularRepositoryService;
import com.koval.githubreposcorer.service.RepositoryScoringService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class PopularRepositoryServiceTest {

    @Mock
    private GithubSearchService githubSearchService;

    @Mock
    private RepositoryScoringService scoringService;

    @InjectMocks
    private PopularRepositoryService service;

    private static final LocalDate DATE = LocalDate.now().minusMonths(6);
    private static final String LANG = "Java";

    @BeforeEach
    void stubScoring() {
        // Pass repos through as ScoredRepository so merge/dedup results are observable
        lenient().when(scoringService.score(anyList())).thenAnswer(inv -> {
            List<RepositoryItemResponse> repos = inv.getArgument(0);
            return repos.stream().map(r -> new ScoredRepository(r, 0.5)).toList();
        });
    }

    @Test
    void noOverlap_allReposMerged() {
        var starred = List.of(repo(1, "starred-only", 1000, 100));
        var forked  = List.of(repo(2, "forked-only",   500, 900));

        when(githubSearchService.fetchTopStarred(LANG, DATE)).thenReturn(starred);
        when(githubSearchService.fetchTopForked(LANG, DATE)).thenReturn(forked);

        var result = service.getPopularRepos(LANG, DATE);

        assertEquals(2, result.items().size());
        var ids = result.items().stream().map(PopularRepositoryResponse::id).toList();
        assertTrue(ids.contains(1L));
        assertTrue(ids.contains(2L));
    }

    @Test
    void duplicate_starredVersionKept() {
        // Same id in both lists — starred entry must win (putIfAbsent)
        var fromStarred = repo(1, "from-starred", 2000, 100);
        var fromForked  = repo(1, "from-forked",   2000, 100);

        when(githubSearchService.fetchTopStarred(LANG, DATE)).thenReturn(List.of(fromStarred));
        when(githubSearchService.fetchTopForked(LANG, DATE)).thenReturn(List.of(fromForked));

        var result = service.getPopularRepos(LANG, DATE);

        assertEquals(1, result.items().size());
        assertEquals(2000, result.items().getFirst().stars());
    }

    @Test
    void bothListsEmpty_returnsEmpty() {
        when(githubSearchService.fetchTopStarred(LANG, DATE)).thenReturn(List.of());
        when(githubSearchService.fetchTopForked(LANG, DATE)).thenReturn(List.of());

        assertTrue(service.getPopularRepos(LANG, DATE).items().isEmpty());
    }

    @Test
    void emptyStarred_forkedReposPresent() {
        var forked = List.of(repo(1, "forked-only", 100, 500));

        when(githubSearchService.fetchTopStarred(LANG, DATE)).thenReturn(List.of());
        when(githubSearchService.fetchTopForked(LANG, DATE)).thenReturn(forked);

        var result = service.getPopularRepos(LANG, DATE);

        assertEquals(1, result.items().size());
        assertEquals(1L, result.items().getFirst().id());
    }

    @Test
    void emptyForked_starredReposPresent() {
        var starred = List.of(repo(1, "starred-only", 1000, 50));

        when(githubSearchService.fetchTopStarred(LANG, DATE)).thenReturn(starred);
        when(githubSearchService.fetchTopForked(LANG, DATE)).thenReturn(List.of());

        var result = service.getPopularRepos(LANG, DATE);

        assertEquals(1, result.items().size());
        assertEquals(1L, result.items().getFirst().id());
    }

    @Test
    void partialOverlap_noDuplicatesInResult() {
        var r1 = repo(1, "repo1", 1000, 200);
        var r2 = repo(2, "repo2",  800, 400);
        var r3 = repo(3, "repo3",  300, 900);

        when(githubSearchService.fetchTopStarred(LANG, DATE)).thenReturn(List.of(r1, r2));
        when(githubSearchService.fetchTopForked(LANG, DATE)).thenReturn(List.of(r2, r3)); // r2 is duplicate

        var result = service.getPopularRepos(LANG, DATE);

        var ids = result.items().stream().map(PopularRepositoryResponse::id).toList();
        assertEquals(3, ids.size());
        assertEquals(3, ids.stream().distinct().count());
    }

    // --- language validation ---

    @ParameterizedTest
    @EnumSource(SupportedLanguage.class)
    void allSupportedLanguages_areAcceptedByService(SupportedLanguage language) {
        when(githubSearchService.fetchTopStarred(language.getGithubName(), DATE)).thenReturn(List.of());
        when(githubSearchService.fetchTopForked(language.getGithubName(), DATE)).thenReturn(List.of());

        assertDoesNotThrow(() -> service.getPopularRepos(language.getGithubName(), DATE));
    }

    @Test
    void unsupportedLanguage_throwsIllegalArgument() {
        var ex = assertThrows(IllegalArgumentException.class,
                () -> service.getPopularRepos("SomeOtherLanguage", DATE));
        assertTrue(ex.getMessage().contains("not supported"));
        assertTrue(ex.getMessage().contains("Supported languages:"));
    }

    @Test
    void languageLookupIsCaseInsensitive() {
        when(githubSearchService.fetchTopStarred("Java", DATE)).thenReturn(List.of());
        when(githubSearchService.fetchTopForked("Java", DATE)).thenReturn(List.of());

        assertDoesNotThrow(() -> service.getPopularRepos("java", DATE));
    }

    // --- createdAfter validation ---

    @Test
    void createdAfterOlderThanOneYear_throwsIllegalArgument() {
        LocalDate tooOld = LocalDate.now().minusYears(1).minusDays(1);
        var ex = assertThrows(IllegalArgumentException.class,
                () -> service.getPopularRepos(LANG, tooOld));
        assertTrue(ex.getMessage().contains("1 year"));
    }

    @Test
    void createdAfterExactlyOneYearAgo_isAllowed() {
        LocalDate boundary = LocalDate.now().minusYears(1);
        when(githubSearchService.fetchTopStarred(LANG, boundary)).thenReturn(List.of());
        when(githubSearchService.fetchTopForked(LANG, boundary)).thenReturn(List.of());

        assertDoesNotThrow(() -> service.getPopularRepos(LANG, boundary));
    }

    private static RepositoryItemResponse repo(long id, String name, int stars, int forks) {
        Instant pushed = Instant.now().minus(365, ChronoUnit.DAYS);
        return new RepositoryItemResponse(id, name, "https://github.com/" + name,
                LANG, stars, forks, pushed, pushed);
    }
}
