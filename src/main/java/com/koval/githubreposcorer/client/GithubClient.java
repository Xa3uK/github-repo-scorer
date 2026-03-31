package com.koval.githubreposcorer.client;

import com.koval.githubreposcorer.model.github.TopRepositoriesResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
public class GithubClient {

    private final RestClient restClient;

    public GithubClient(@Value("${github.api.token:}") String token) {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://api.github.com")
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28");

        if (StringUtils.hasText(token)) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }

        this.restClient = builder.build();
    }

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
