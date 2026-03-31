package com.koval.githubreposcorer.api.controller;

import com.koval.githubreposcorer.api.dto.PopularRepositoryResponse;
import com.koval.githubreposcorer.service.popular.PopularRepositoryService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/repositories")
@Validated
public class RepositoryScoreController {

    private final PopularRepositoryService popularRepositoryService;

    public RepositoryScoreController(PopularRepositoryService popularRepositoryService) {
        this.popularRepositoryService = popularRepositoryService;
    }

    @GetMapping("/popular")
    public List<PopularRepositoryResponse> getPopularRepos(
        @RequestParam
        @NotBlank
        String language,
        @RequestParam
        @NotNull
        @PastOrPresent
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdAfter
    ) {
        return popularRepositoryService.getPopularRepos(language, createdAfter).items();
    }
}
