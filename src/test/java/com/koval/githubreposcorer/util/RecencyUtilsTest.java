package com.koval.githubreposcorer.util;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecencyUtilsTest {

    @Test
    void null_returnsZero() {
        assertEquals(0.0, RecencyUtils.recencyScore(null));
    }

    @Test
    void today_returnsOne() {
        assertEquals(1.0, RecencyUtils.recencyScore(Instant.now()));
    }

    @Test
    void sevenDaysAgo_returnsOne() {
        assertEquals(1.0, RecencyUtils.recencyScore(daysAgo(7)));
    }

    @Test
    void eightDaysAgo_returnsPointEight() {
        assertEquals(0.8, RecencyUtils.recencyScore(daysAgo(8)));
    }

    @Test
    void thirtyDaysAgo_returnsPointEight() {
        assertEquals(0.8, RecencyUtils.recencyScore(daysAgo(30)));
    }

    @Test
    void thirtyOneDaysAgo_returnsPointFive() {
        assertEquals(0.5, RecencyUtils.recencyScore(daysAgo(31)));
    }

    @Test
    void ninetyDaysAgo_returnsPointFive() {
        assertEquals(0.5, RecencyUtils.recencyScore(daysAgo(90)));
    }

    @Test
    void ninetyOneDaysAgo_returnsPointTwo() {
        assertEquals(0.2, RecencyUtils.recencyScore(daysAgo(91)));
    }

    @Test
    void oneHundredEightyDaysAgo_returnsPointTwo() {
        assertEquals(0.2, RecencyUtils.recencyScore(daysAgo(180)));
    }

    @Test
    void oneHundredEightyOneDaysAgo_returnsZero() {
        assertEquals(0.0, RecencyUtils.recencyScore(daysAgo(181)));
    }

    @Test
    void oneYearAgo_returnsZero() {
        assertEquals(0.0, RecencyUtils.recencyScore(daysAgo(365)));
    }

    private static Instant daysAgo(long days) {
        return Instant.now().minus(days, ChronoUnit.DAYS);
    }
}
