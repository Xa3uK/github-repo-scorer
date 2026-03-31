package com.koval.githubreposcorer.service;

import com.koval.githubreposcorer.client.GithubClient;
import com.koval.githubreposcorer.model.github.RepositoryItemResponse;
import com.koval.githubreposcorer.model.github.TopRepositoriesResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class GithubSearchService {

    private final GithubClient githubClient;

    public GithubSearchService(GithubClient githubClient) {
        this.githubClient = githubClient;
    }

    public List<RepositoryItemResponse> fetchTopStarred(String language, LocalDate createdAfter) {
        return fetch(language, createdAfter, "stars");
    }

    public List<RepositoryItemResponse> fetchTopForked(String language, LocalDate createdAfter) {
        return fetch(language, createdAfter, "forks");
    }

    private List<RepositoryItemResponse> fetch(String language, LocalDate createdAfter, String sort) {
        String query = "language:" + language + " created:>=" + createdAfter;
        TopRepositoriesResponse response = githubClient.searchRepositories(query, sort);

        if (response == null || response.items() == null) {
            return List.of();
        }

        return response.items();
    }
}
