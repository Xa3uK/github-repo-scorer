package com.koval.githubreposcorer.api.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleValidation(ConstraintViolationException e) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setDetail(e.getMessage());
        return ResponseEntity.badRequest().body(pd);
    }

    @ExceptionHandler(GithubServerException.class)
    public ResponseEntity<ProblemDetail> handleGithubServerError(GithubServerException e) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_GATEWAY);
        pd.setDetail("GitHub API error after retries: " + e.getStatusCode());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(pd);
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<ProblemDetail> handleTimeout() {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.SERVICE_UNAVAILABLE);
        pd.setDetail("GitHub API unreachable after retries.");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(pd);
    }

    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<ProblemDetail> handleGithubError(RestClientResponseException e) {
        if (e.getStatusCode().value() == 429) {
            ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.TOO_MANY_REQUESTS);
            pd.setDetail("GitHub rate limit exceeded. Provide GITHUB_API_TOKEN or retry later.");
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(pd);
        }
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_GATEWAY);
        pd.setDetail("GitHub API error: " + e.getStatusCode());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(pd);
    }
}
