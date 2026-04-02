package com.koval.githubreposcorer.api.controller;

import com.koval.githubreposcorer.api.response.PopularRepositoryResponse;
import com.koval.githubreposcorer.service.PopularRepositoryService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/repositories")
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
        @Pattern(
            regexp = "^[A-Za-z][A-Za-z0-9#+.\\-\\s]{0,49}$",
            message = "Language must start with a letter and be at most 50 characters"
        )
        String language,
        @RequestParam
        @NotNull
        @PastOrPresent
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate createdAfter
    ) {
        log.info("Incoming request: language={}, createdAfter={}", language.trim(), createdAfter);
        return popularRepositoryService.getPopularRepos(language.trim(), createdAfter).items();
    }
}
