package com.koval.githubreposcorer.unit;

import com.koval.githubreposcorer.model.github.RepositoryItemResponse;
import com.koval.githubreposcorer.model.result.ScoredRepository;
import com.koval.githubreposcorer.service.RepositoryScoringService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RepositoryScoringServiceTest {

    private final RepositoryScoringService service = new RepositoryScoringService();

    // --- empty input ---

    @Test
    void emptyList_returnsEmpty() {
        assertTrue(service.score(List.of()).isEmpty());
    }

    // --- score formula ---

    @Test
    void singleRepo_scoreIsCorrect() {
        // Only one repo → starsP95 = stars, forksP95 = forks → starScore = forkScore = 1.0
        // pushedAt = now → recencyScore = 1.0
        // score = 0.65*1 + 0.25*1 + 0.10*1 = 1.0
        var repo = repo(1, "a", 1000, 500, daysAgo(0));
        var results = service.score(List.of(repo));

        assertEquals(1, results.size());
        assertEquals(1.0, results.getFirst().score());
    }

    @Test
    void starScore_cappedAtOne() {
        // repo with stars above P95 still gets starScore = 1.0
        var below = repo(1, "below", 100, 0, daysAgo(200));
        var above = repo(2, "above", 10_000, 0, daysAgo(200));
        // P95 of [100, 10000] with 2 elements: ceil(0.95*2)-1 = index 1 → 10000
        // above: starScore = min(1, 10000/10000) = 1.0 — not exceeding
        // but if above has more stars than P95 it's still capped — test with 3 elements
        var outlier = repo(3, "outlier", 999_999, 0, daysAgo(200));
        var results = service.score(List.of(below, above, outlier));
        // outlier starScore = min(1.0, 999999 / p95) ≤ 1.0
        results.forEach(r -> assertTrue(r.score() <= 1.0));
    }

    @Test
    void recencyScore_affectsTotal() {
        // Two repos with same stars/forks; one pushed recently, one long ago
        // They share the same P95, so star/forkScores are equal — only recency differs
        var recent = repo(1, "recent", 500, 200, daysAgo(3));   // recency = 1.0
        var old    = repo(2, "old",    500, 200, daysAgo(365));  // recency = 0.0

        var results = service.score(List.of(recent, old));

        ScoredRepository recentScored = find(results, 1);
        ScoredRepository oldScored   = find(results, 2);

        // diff should be 0.10 * (1.0 - 0.0) = 0.10
        assertEquals(0.10, recentScored.score() - oldScored.score(), 1e-9);
    }

    // --- sorting ---

    @Test
    void sortedByScoreDescending() {
        var high = repo(1, "high", 1000, 500, daysAgo(1));
        var low  = repo(2, "low",  10,   5,   daysAgo(200));

        var results = service.score(List.of(low, high));

        assertEquals(1L, results.get(0).repo().id());
        assertEquals(2L, results.get(1).repo().id());
    }

    @Test
    void tieBreak_byStarsThenForks() {
        // Same score achievable only if recency matches too — use same pushedAt
        Instant pushed = daysAgo(200); // recency = 0.0 for all, avoids time flakiness
        // Make stars/forks proportional so scores are equal:
        // P95 of 3 elements [100,100,100] = 100; all get starScore=1, forkScore=1 → score equal
        var moreStars  = repo(1, "moreStars",  100, 50, pushed);
        var fewerStars = repo(2, "fewerStars", 100, 20, pushed);
        var least      = repo(3, "least",      100, 10, pushed);

        var results = service.score(List.of(least, fewerStars, moreStars));

        // scores are equal; tie-break: stars equal → forks desc
        assertEquals(1L, results.get(0).repo().id()); // 50 forks
        assertEquals(2L, results.get(1).repo().id()); // 20 forks
        assertEquals(3L, results.get(2).repo().id()); // 10 forks
    }

    @Test
    void tieBreak_byPushedAtWhenStarsAndForksEqual() {
        Instant older  = daysAgo(300);
        Instant newer  = daysAgo(250);
        // recency = 0.0 for both (>180 days); same stars and forks → same score
        var repoNewer = repo(1, "newer", 100, 50, newer);
        var repoOlder = repo(2, "older", 100, 50, older);

        var results = service.score(List.of(repoOlder, repoNewer));

        assertEquals(1L, results.get(0).repo().id()); // pushed more recently
        assertEquals(2L, results.get(1).repo().id());
    }

    // --- limit ---

    @Test
    void moreThanHundredRepos_returnsOnlyTop100() {
        List<RepositoryItemResponse> repos = new ArrayList<>();
        for (int i = 1; i <= 150; i++) {
            repos.add(repo(i, "repo" + i, i, i, daysAgo(200)));
        }
        assertEquals(100, service.score(repos).size());
    }

    @Test
    void moreThanHundredRepos_top100HaveHighestScores() {
        // stars and forks scale with i → score increases with i
        // top 100 out of 150 = ids 51..150; ids 1..50 are cut
        List<RepositoryItemResponse> repos = new ArrayList<>();
        for (int i = 1; i <= 150; i++) {
            repos.add(repo(i, "repo" + i, i * 10, i, daysAgo(200)));
        }
        var returnedIds = service.score(repos).stream().map(r -> r.repo().id()).toList();

        for (long id = 1; id <= 50; id++) {
            assertFalse(returnedIds.contains(id), "repo " + id + " should have been cut");
        }
        for (long id = 51; id <= 150; id++) {
            assertTrue(returnedIds.contains(id), "repo " + id + " should be in top 100");
        }
    }

    // --- helpers ---

    private static RepositoryItemResponse repo(long id, String name, int stars, int forks, Instant pushedAt) {
        return new RepositoryItemResponse(id, name, "https://github.com/" + name, "Java",
                stars, forks, daysAgo(365), pushedAt);
    }

    private static Instant daysAgo(long days) {
        return Instant.now().minus(days, ChronoUnit.DAYS);
    }

    private static ScoredRepository find(List<ScoredRepository> results, long id) {
        return results.stream().filter(r -> r.repo().id() == id).findFirst().orElseThrow();
    }
}
