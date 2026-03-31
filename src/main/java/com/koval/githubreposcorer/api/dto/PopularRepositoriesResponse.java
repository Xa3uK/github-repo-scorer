package com.koval.githubreposcorer.api.dto;

import java.util.List;

public record PopularRepositoriesResponse(
    List<PopularRepositoryResponse> items
) {}
