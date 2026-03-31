package com.koval.githubreposcorer.service;

import com.koval.githubreposcorer.api.response.PopularRepositoriesResponse;
import com.koval.githubreposcorer.api.response.PopularRepositoryResponse;
import com.koval.githubreposcorer.model.github.RepositoryItemResponse;
import com.koval.githubreposcorer.model.result.ScoredRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PopularRepositoryService {

    private final GithubSearchService githubSearchService;
    private final RepositoryScoringService scoringService;

    public PopularRepositoryService(GithubSearchService githubSearchService,
                                    RepositoryScoringService scoringService) {
        this.githubSearchService = githubSearchService;
        this.scoringService = scoringService;
    }

    @Cacheable(value = "popularRepositories", key = "#language + ':' + #createdAfter")
    public PopularRepositoriesResponse getPopularRepos(String language, LocalDate createdAfter) {
        List<RepositoryItemResponse> mostStarred = githubSearchService.fetchTopStarred(language, createdAfter);
        List<RepositoryItemResponse> mostForked  = githubSearchService.fetchTopForked(language, createdAfter);

        Map<Long, RepositoryItemResponse> merged = new LinkedHashMap<>();
        mostStarred.forEach(r -> merged.put(r.id(), r));
        mostForked.forEach(r -> merged.putIfAbsent(r.id(), r));

        List<PopularRepositoryResponse> items = scoringService.score(List.copyOf(merged.values()))
                .stream()
                .map(this::toResponse)
                .toList();

        return new PopularRepositoriesResponse(items);
    }

    private PopularRepositoryResponse toResponse(ScoredRepository scored) {
        RepositoryItemResponse repo = scored.repo();
        return new PopularRepositoryResponse(
                repo.id(),
                repo.fullName(),
                repo.htmlUrl(),
                repo.language(),
                repo.stargazersCount(),
                repo.forksCount(),
                repo.createdAt(),
                repo.pushedAt(),
                Math.round(scored.score() * 10000.0) / 10000.0
        );
    }
}
