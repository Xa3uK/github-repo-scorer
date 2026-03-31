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

    public List<RepositoryItemResponse> fetchTopStarred(String language, LocalDate createdFrom) {
        return fetch(language, createdFrom, "stars");
    }

    public List<RepositoryItemResponse> fetchTopForked(String language, LocalDate createdFrom) {
        return fetch(language, createdFrom, "forks");
    }

    private List<RepositoryItemResponse> fetch(String language, LocalDate createdFrom, String sort) {
        String query = "language:" + language + " created:>=" + createdFrom;
        TopRepositoriesResponse response = githubClient.searchRepositories(query, sort);

        if (response == null || response.items() == null) {
            return List.of();
        }

        return response.items();
    }
}
