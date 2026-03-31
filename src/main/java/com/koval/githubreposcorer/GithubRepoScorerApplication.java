package com.koval.githubreposcorer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class GithubRepoScorerApplication {

    public static void main(String[] args) {
        SpringApplication.run(GithubRepoScorerApplication.class, args);
    }

}
