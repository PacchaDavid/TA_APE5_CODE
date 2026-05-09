package com.ta_ape5.af_conversion_minimization.model;

import java.util.List;
import java.util.Map;

public record AutomatonDefinition(
        int exerciseId,
        String name,
        AutomataType automataType,
        List<String> states,
        List<String> alphabet,
        String initialState,
        List<String> acceptingStates,
        Map<String, Map<String, List<String>>> transitions
) {
}