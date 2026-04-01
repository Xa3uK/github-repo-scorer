package com.koval.githubreposcorer.api.exception;

import lombok.Getter;
import org.springframework.http.HttpStatusCode;

@Getter
public class GithubServerException extends RuntimeException {

    private final HttpStatusCode statusCode;

    public GithubServerException(HttpStatusCode statusCode) {
        super("GitHub server error: " + statusCode);
        this.statusCode = statusCode;
    }
}
