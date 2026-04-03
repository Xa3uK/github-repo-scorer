package com.koval.githubreposcorer.web;

import com.koval.githubreposcorer.api.exception.GithubServerException;
import com.koval.githubreposcorer.api.response.PopularRepositoriesResponse;
import com.koval.githubreposcorer.api.response.PopularRepositoryResponse;
import com.koval.githubreposcorer.model.domain.SupportedLanguage;
import com.koval.githubreposcorer.service.PopularRepositoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK,
                properties = "spring.cache.type=simple")
class RepositoryScoreControllerTest {

    private static final String URL = "/api/v1/repositories/popular";
    private static final String RECENT_DATE = LocalDate.now().minusMonths(6).toString();

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private PopularRepositoryService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    // --- supported languages ---

    @ParameterizedTest
    @EnumSource(SupportedLanguage.class)
    void allSupportedLanguages_return200(SupportedLanguage language) throws Exception {
        when(service.getPopularRepos(eq(language.getGithubName()), any()))
                .thenReturn(new PopularRepositoriesResponse(List.of()));

        mockMvc.perform(get(URL)
                        .param("language", language.getGithubName())
                        .param("createdAfter", RECENT_DATE))
                .andExpect(status().isOk());
    }

    // --- happy path ---

    @Test
    void validRequest_returns200WithItems() throws Exception {
        var item = new PopularRepositoryResponse(
                1L, "owner/repo", "https://github.com/owner/repo", "Java",
                1000, 200,
                Instant.parse("2025-01-01T00:00:00Z"),
                Instant.parse("2025-06-01T00:00:00Z"),
                0.9125);

        when(service.getPopularRepos(eq("Java"), any(LocalDate.class)))
                .thenReturn(new PopularRepositoriesResponse(List.of(item)));

        mockMvc.perform(get(URL)
                        .param("language", "Java")
                        .param("createdAfter", RECENT_DATE))
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
                        .param("createdAfter", RECENT_DATE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void languageWithSpecialChars_returns200() throws Exception {
        when(service.getPopularRepos(eq("C++"), any(LocalDate.class)))
                .thenReturn(new PopularRepositoriesResponse(List.of()));

        mockMvc.perform(get(URL)
                        .param("language", "C++")
                        .param("createdAfter", RECENT_DATE))
                .andExpect(status().isOk());
    }

    // --- language validation (400) ---

    @Test
    void missingLanguage_returns400() throws Exception {
        mockMvc.perform(get(URL).param("createdAfter", RECENT_DATE))
                .andExpect(status().isBadRequest());
    }

    @Test
    void blankLanguage_returns400() throws Exception {
        mockMvc.perform(get(URL)
                        .param("language", "   ")
                        .param("createdAfter", RECENT_DATE))
                .andExpect(status().isBadRequest());
    }

    // --- createdAfter validation (400) ---

    @Test
    void missingCreatedAfter_returns400() throws Exception {
        mockMvc.perform(get(URL).param("language", "Java"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void futureCreatedAfter_returns400() throws Exception {
        mockMvc.perform(get(URL)
                        .param("language", "Java")
                        .param("createdAfter", "2030-01-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void serviceThrowsIllegalArgument_returns400() throws Exception {
        when(service.getPopularRepos(any(), any()))
                .thenThrow(new IllegalArgumentException("createdAfter must not be older than 1 year"));

        mockMvc.perform(get(URL)
                        .param("language", "Java")
                        .param("createdAfter", RECENT_DATE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("1 year")));
    }

    // --- error handling ---

    @Test
    void githubRateLimit_returns429() throws Exception {
        when(service.getPopularRepos(any(), any()))
                .thenThrow(new org.springframework.web.client.HttpClientErrorException(
                        HttpStatus.TOO_MANY_REQUESTS));

        mockMvc.perform(get(URL)
                        .param("language", "Java")
                        .param("createdAfter", RECENT_DATE))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.detail").value(containsString("rate limit")));
    }

    @Test
    void githubServerError_returns502() throws Exception {
        when(service.getPopularRepos(any(), any()))
                .thenThrow(new GithubServerException(HttpStatus.BAD_GATEWAY));

        mockMvc.perform(get(URL)
                        .param("language", "Java")
                        .param("createdAfter", RECENT_DATE))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.detail").value(containsString("GitHub API error after retries")));
    }

    @Test
    void githubTimeout_returns503() throws Exception {
        when(service.getPopularRepos(any(), any()))
                .thenThrow(new ResourceAccessException("Connection timed out"));

        mockMvc.perform(get(URL)
                        .param("language", "Java")
                        .param("createdAfter", RECENT_DATE))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value("GitHub API unreachable after retries."));
    }
}
