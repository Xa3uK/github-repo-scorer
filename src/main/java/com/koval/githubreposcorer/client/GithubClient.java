package com.koval.githubreposcorer.client;

import com.koval.githubreposcorer.api.exception.GithubServerException;
import com.koval.githubreposcorer.model.github.TopRepositoriesResponse;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Component
public class GithubClient {

    private final RestClient restClient;

    public GithubClient(RestClient githubRestClient) {
        this.restClient = githubRestClient;
    }

    @Retryable(
        includes = {ResourceAccessException.class, GithubServerException.class},
        maxRetries = 2,
        delay = 500,
        multiplier = 2.0
    )
    public TopRepositoriesResponse searchRepositories(String query, String sort) {
        return restClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/search/repositories")
                .queryParam("q", query)
                .queryParam("sort", sort)
                .queryParam("order", "desc")
                .queryParam("per_page", 100)
                .queryParam("page", 1)
                .build())
            .retrieve()
            .body(TopRepositoriesResponse.class);
    }
}