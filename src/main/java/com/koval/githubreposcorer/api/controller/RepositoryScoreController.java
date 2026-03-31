package com.koval.githubreposcorer.api.controller;

import com.koval.githubreposcorer.api.dto.PopularRepositoryResponse;
import com.koval.githubreposcorer.service.popular.PopularRepositoryService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

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
    public List<PopularRepositoryResponse> getPopular(
            @RequestParam @NotBlank String language,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdAfter
    ) {
        if (createdAfter.isAfter(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "createdAfter cannot be in the future");
        }
        return popularRepositoryService.getCandidates(language, createdAfter);
    }
}
