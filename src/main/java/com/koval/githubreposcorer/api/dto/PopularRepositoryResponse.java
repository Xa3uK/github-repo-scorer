package com.koval.githubreposcorer.api.dto;

import java.time.Instant;

public record PopularRepositoryResponse(
        long id,
        String fullName,
        String htmlUrl,
        String language,
        int stars,
        int forks,
        Instant createdAt,
        Instant pushedAt,
        double score
) {}
