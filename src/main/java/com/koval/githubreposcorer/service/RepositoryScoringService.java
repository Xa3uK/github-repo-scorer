package com.koval.githubreposcorer.service;

import com.koval.githubreposcorer.model.github.RepositoryItemResponse;
import com.koval.githubreposcorer.model.result.ScoredRepository;
import com.koval.githubreposcorer.util.RecencyUtils;
import com.koval.githubreposcorer.util.PercentileUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
public class RepositoryScoringService {

    public List<ScoredRepository> score(List<RepositoryItemResponse> candidates) {
        if (candidates.isEmpty()) {
            return List.of();
        }

        double starsP95 = Math.max(1, PercentileUtils.percentile95(
                candidates.stream().map(RepositoryItemResponse::stargazersCount).toList()
        ));
        double forksP95 = Math.max(1, PercentileUtils.percentile95(
                candidates.stream().map(RepositoryItemResponse::forksCount).toList()
        ));
        log.info("P95 computed: starsP95={}, forksP95={}, candidates={}", starsP95, forksP95, candidates.size());

        return candidates.stream()
                .map(repo -> {
                    double starScore    = Math.min(1.0, repo.stargazersCount() / starsP95);
                    double forkScore    = Math.min(1.0, repo.forksCount()      / forksP95);
                    double recencyScore = RecencyUtils.recencyScore(repo.pushedAt());
                    double score = 0.65 * starScore + 0.25 * forkScore + 0.10 * recencyScore;
                    return new ScoredRepository(repo, score);
                })
                .sorted(
                        Comparator.comparingDouble(ScoredRepository::score).reversed()
                                .thenComparing(Comparator.comparingInt((ScoredRepository s) -> s.repo().stargazersCount()).reversed())
                                .thenComparing(Comparator.comparingInt((ScoredRepository s) -> s.repo().forksCount()).reversed())
                                .thenComparing((ScoredRepository s) -> s.repo().pushedAt(),
                                        Comparator.nullsFirst(Comparator.reverseOrder()))
                                .thenComparing((ScoredRepository s) -> s.repo().createdAt(),
                                        Comparator.nullsFirst(Comparator.reverseOrder()))
                )
                .limit(100)
                .toList();
    }
}
