package com.koval.githubreposcorer.util;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class RecencyUtils {

    private RecencyUtils() {}

    public static double recencyScore(Instant pushedAt) {
        if (pushedAt == null) {
            return 0.0;
        }
        long days = ChronoUnit.DAYS.between(pushedAt, Instant.now());
        if (days <= 7)   return 1.0;
        if (days <= 30)  return 0.8;
        if (days <= 90)  return 0.5;
        if (days <= 180) return 0.2;
        return 0.0;
    }
}
