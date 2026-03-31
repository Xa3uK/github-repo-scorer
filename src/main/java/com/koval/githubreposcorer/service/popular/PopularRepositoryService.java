package com.koval.githubreposcorer.service.popular;

import com.koval.githubreposcorer.api.dto.PopularRepositoryResponse;
import com.koval.githubreposcorer.model.github.RepositoryItemResponse;
import com.koval.githubreposcorer.service.GithubSearchService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PopularRepositoryService {

    private final GithubSearchService githubSearchService;

    public PopularRepositoryService(GithubSearchService githubSearchService) {
        this.githubSearchService = githubSearchService;
    }

    public List<PopularRepositoryResponse> getCandidates(String language, LocalDate createdAfter) {
        List<RepositoryItemResponse> starred = githubSearchService.fetchTopStarred(language, createdAfter);
        List<RepositoryItemResponse> forked = githubSearchService.fetchTopForked(language, createdAfter);

        Map<Long, RepositoryItemResponse> merged = new LinkedHashMap<>();
        starred.forEach(r -> merged.put(r.id(), r));
        forked.forEach(r -> merged.putIfAbsent(r.id(), r));

        return merged.values().stream()
                .map(this::toResponse)
                .toList();
    }

    private PopularRepositoryResponse toResponse(RepositoryItemResponse repo) {
        return new PopularRepositoryResponse(
                repo.id(),
                repo.fullName(),
                repo.htmlUrl(),
                repo.language(),
                repo.stargazersCount(),
                repo.forksCount(),
                repo.createdAt(),
                repo.pushedAt(),
                0.0
        );
    }
}
