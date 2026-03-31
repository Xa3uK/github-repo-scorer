package com.koval.githubreposcorer.model.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RepositoryItemResponse(
        long id,
        @JsonProperty("full_name") String fullName,
        @JsonProperty("html_url") String htmlUrl,
        String language,
        @JsonProperty("stargazers_count") int stargazersCount,
        @JsonProperty("forks_count") int forksCount,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("pushed_at") Instant pushedAt
) {}
