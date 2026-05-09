package com.ta_ape5.af_conversion_minimization.model;

public record AutomataTestResult(
        int index,
        String input,
        boolean afndAccepted,
        boolean afdAccepted,
        boolean afdMinAccepted,
        boolean equivalent
) {
}