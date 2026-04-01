package com.koval.githubreposcorer.config;

import com.koval.githubreposcorer.api.exception.GithubServerException;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Configuration
public class GithubClientConfig {

    @Bean
    public RestClient githubRestClient(
        @Value("${github.api.base-url:https://api.github.com}") String baseUrl,
        @Value("${github.api.token:}") String token,
        @Value("${github.api.connect-timeout:3s}") Duration connectTimeout,
        @Value("${github.api.read-timeout:10s}") Duration readTimeout) {

        HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(connectTimeout)
            .build();

        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(readTimeout);

        RestClient.Builder builder = RestClient.builder()
            .requestFactory(factory)
            .baseUrl(baseUrl)
            .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
            .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
            .defaultStatusHandler(HttpStatusCode::is5xxServerError, (request, response) -> {
                throw new GithubServerException(response.getStatusCode());
            });

        if (StringUtils.hasText(token)) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }

        return builder.build();
    }
}
