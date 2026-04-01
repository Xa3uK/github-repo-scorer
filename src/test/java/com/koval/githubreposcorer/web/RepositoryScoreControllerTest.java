package com.koval.githubreposcorer.web;

import com.koval.githubreposcorer.api.controller.RepositoryScoreController;
import com.koval.githubreposcorer.api.exception.GlobalExceptionHandler;
import com.koval.githubreposcorer.api.exception.GithubServerException;
import com.koval.githubreposcorer.api.response.PopularRepositoriesResponse;
import com.koval.githubreposcorer.api.response.PopularRepositoryResponse;
import com.koval.githubreposcorer.service.PopularRepositoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.ResourceAccessException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RepositoryScoreControllerTest {

    private static final String URL = "/api/v1/repositories/popular";

    private MockMvc mockMvc;
    private PopularRepositoryService service;

    @BeforeEach
    void setUp() {
        service = mock(PopularRepositoryService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new RepositoryScoreController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // --- happy path ---

    @Test
    void validRequest_returns200WithItems() throws Exception {
        var item = new PopularRepositoryResponse(
                1L, "owner/repo", "https://github.com/owner/repo", "Java",
                1000, 200,
                Instant.parse("2024-01-01T00:00:00Z"),
                Instant.parse("2024-06-01T00:00:00Z"),
                0.9125);

        when(service.getPopularRepos(eq("Java"), any(LocalDate.class)))
                .thenReturn(new PopularRepositoriesResponse(List.of(item)));

        mockMvc.perform(get(URL)
                        .param("language", "Java")
                        .param("createdAfter", "2024-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].fullName").value("owner/repo"))
                .andExpect(jsonPath("$[0].score").value(0.9125));
    }

    @Test
    void validRequest_emptyResult_returns200WithEmptyArray() throws Exception {
        when(service.getPopularRepos(any(), any()))
                .thenReturn(new PopularRepositoriesResponse(List.of()));

        mockMvc.perform(get(URL)
                        .param("language", "Kotlin")
                        .param("createdAfter", "2024-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // --- validation (400) ---

    @Test
    void missingLanguage_returns400() throws Exception {
        mockMvc.perform(get(URL).param("createdAfter", "2024-01-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingCreatedAfter_returns400() throws Exception {
        mockMvc.perform(get(URL).param("language", "Java"))
                .andExpect(status().isBadRequest());
    }

    // --- error handling ---

    @Test
    void githubRateLimit_returns429() throws Exception {
        when(service.getPopularRepos(any(), any()))
                .thenThrow(new org.springframework.web.client.HttpClientErrorException(
                        HttpStatus.TOO_MANY_REQUESTS));

        mockMvc.perform(get(URL)
                        .param("language", "Java")
                        .param("createdAfter", "2024-01-01"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.detail").value(containsString("rate limit")));
    }

    @Test
    void githubServerError_returns502() throws Exception {
        when(service.getPopularRepos(any(), any()))
                .thenThrow(new GithubServerException(HttpStatus.BAD_GATEWAY));

        mockMvc.perform(get(URL)
                        .param("language", "Java")
                        .param("createdAfter", "2024-01-01"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.detail").value(containsString("GitHub API error after retries")));
    }

    @Test
    void githubTimeout_returns503() throws Exception {
        when(service.getPopularRepos(any(), any()))
                .thenThrow(new ResourceAccessException("Connection timed out"));

        mockMvc.perform(get(URL)
                        .param("language", "Java")
                        .param("createdAfter", "2024-01-01"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value("GitHub API unreachable after retries."));
    }
}
