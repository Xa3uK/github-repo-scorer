package com.koval.githubreposcorer.model.result;

import com.koval.githubreposcorer.model.github.RepositoryItemResponse;

public record ScoredRepository(RepositoryItemResponse repo, double score) {}
