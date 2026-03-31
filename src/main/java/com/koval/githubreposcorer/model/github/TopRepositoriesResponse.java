package com.koval.githubreposcorer.model.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TopRepositoriesResponse(List<RepositoryItemResponse> items) {}
