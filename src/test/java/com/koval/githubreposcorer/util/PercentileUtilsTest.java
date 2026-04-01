package com.koval.githubreposcorer.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PercentileUtilsTest {

    @Test
    void emptyList_returnsOne() {
        assertEquals(1.0, PercentileUtils.percentile95(List.of()));
    }

    @Test
    void singleElement_returnsThatElement() {
        assertEquals(42.0, PercentileUtils.percentile95(List.of(42)));
    }

    @Test
    void twoElements_returnsHigher() {
        // ceil(0.95 * 2) - 1 = ceil(1.9) - 1 = 2 - 1 = index 1 → 20
        assertEquals(20.0, PercentileUtils.percentile95(List.of(10, 20)));
    }

    @Test
    void hundredElements_returnsNinetyFifthValue() {
        // 1..100 sorted; ceil(0.95 * 100) - 1 = 95 - 1 = index 94 → value 95
        List<Integer> values = new java.util.ArrayList<>();
        for (int i = 1; i <= 100; i++) values.add(i);
        assertEquals(95.0, PercentileUtils.percentile95(values));
    }

    @Test
    void unsortedInput_sortsBeforeCalculating() {
        // same 1..100 shuffled — result must be the same
        List<Integer> values = new java.util.ArrayList<>();
        for (int i = 100; i >= 1; i--) values.add(i);
        assertEquals(95.0, PercentileUtils.percentile95(values));
    }

    @Test
    void allSameValues_returnsThatValue() {
        assertEquals(7.0, PercentileUtils.percentile95(List.of(7, 7, 7, 7, 7)));
    }

    @Test
    void twentyElements_returnsTwentiethPercentile95() {
        // ceil(0.95 * 20) - 1 = ceil(19) - 1 = 19 - 1 = index 18 → value 19
        List<Integer> values = new java.util.ArrayList<>();
        for (int i = 1; i <= 20; i++) values.add(i);
        assertEquals(19.0, PercentileUtils.percentile95(values));
    }
}
