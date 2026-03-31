package com.koval.githubreposcorer.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PercentileUtils {

    private PercentileUtils() {}

    public static double percentile95(List<Integer> values) {
        if (values.isEmpty()) {
            return 1.0;
        }
        List<Integer> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int index = (int) Math.ceil(0.95 * sorted.size()) - 1;
        index = Math.clamp(index, 0, sorted.size() - 1);
        return sorted.get(index);
    }
}
