package com.koval.githubreposcorer.api.exception;

import org.springframework.http.HttpStatusCode;

public class GithubServerException extends RuntimeException {

    private final HttpStatusCode statusCode;

    public GithubServerException(HttpStatusCode statusCode) {
        super("GitHub server error: " + statusCode);
        this.statusCode = statusCode;
    }

    public HttpStatusCode getStatusCode() {
        return statusCode;
    }
}
