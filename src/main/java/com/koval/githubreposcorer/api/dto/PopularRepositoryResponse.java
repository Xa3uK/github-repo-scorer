package com.koval.githubreposcorer.api.dto;

import java.time.Instant;

public record PopularRepositoryResponse(
    long id,
    String fullName,
    String htmlUrl,
    String language,
    int stars,
    int forks,

    //repo creation time on GitHub
    Instant createdAt,

    //last time code was pushed to the repo
    Instant pushedAt,
    double score
) {}
