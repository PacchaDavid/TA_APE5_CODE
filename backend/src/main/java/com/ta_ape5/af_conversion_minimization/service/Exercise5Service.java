package com.ta_ape5.af_conversion_minimization.service;

import com.ta_ape5.af_conversion_minimization.model.AutomataTestResult;
import com.ta_ape5.af_conversion_minimization.model.AutomataType;
import com.ta_ape5.af_conversion_minimization.model.AutomatonDefinition;
import com.ta_ape5.af_conversion_minimization.model.SimulationResult;
import com.ta_ape5.af_conversion_minimization.model.SimulationStep;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@Service
public class Exercise5Service implements AutomataService {

    private static final int EXERCISE_ID = 5;

    private static final List<String> AFND_STATES = List.of("q0", "q1", "q2");
    private static final List<String> AFD_STATES = List.of("{q0}", "{q1}", "{q2}", "∅");
    private static final List<String> ALPHABET = List.of("h", "t", "m", "c");

    private static final Map<String, Map<String, List<String>>> AFND_TRANSITIONS = Map.of(
            "q0", Map.of(
                    "h", List.of("q1")
            ),
            "q1", Map.of(
                    "t", List.of("q1"),
                    "m", List.of("q1"),
                    "c", List.of("q2")
            ),
            "q2", Map.of()
    );

    private static final Map<String, Map<String, String>> AFD_TRANSITIONS = Map.of(
            "{q0}", Map.of(
                    "h", "{q1}",
                    "t", "∅",
                    "m", "∅",
                    "c", "∅"
            ),
            "{q1}", Map.of(
                    "h", "∅",
                    "t", "{q1}",
                    "m", "{q1}",
                    "c", "{q2}"
            ),
            "{q2}", Map.of(
                    "h", "∅",
                    "t", "∅",
                    "m", "∅",
                    "c", "∅"
            ),
            "∅", Map.of(
                    "h", "∅",
                    "t", "∅",
                    "m", "∅",
                    "c", "∅"
            )
    );

    private static final List<String> TEST_INPUTS = List.of(
            "",
            "h",
            "hc",
            "htc",
            "hmc",
            "httc",
            "hmmc",
            "htmc",
            "htttmc",
            "c",
            "tc",
            "mc",
            "hth",
            "hcc",
            "ht",
            "hhmtc",
            "htmtmc",
            "htttttc",
            "hmmmmc",
            "htmtmtmtc"
    );

    @Override
    public int exerciseId() {
        return EXERCISE_ID;
    }

    @Override
    public SimulationResult simulate(AutomataType automataType, String input) {
        return switch (automataType) {
            case AFND -> simulateAfnd(input == null ? "" : input);
            case AFD, AFD_MIN -> simulateDeterministic(input == null ? "" : input, "{q0}", AFD_TRANSITIONS, Set.of("{q2}"));
        };
    }

    @Override
    public AutomatonDefinition definition(AutomataType automataType) {
        return switch (automataType) {
            case AFND -> new AutomatonDefinition(EXERCISE_ID, "Ejercicio 5 - AFND", automataType, AFND_STATES, ALPHABET, "q0", List.of("q2"), AFND_TRANSITIONS);
            case AFD -> new AutomatonDefinition(EXERCISE_ID, "Ejercicio 5 - AFD", automataType, AFD_STATES, ALPHABET, "{q0}", List.of("{q2}"), toListTransitionMap(AFD_TRANSITIONS));
            case AFD_MIN -> new AutomatonDefinition(EXERCISE_ID, "Ejercicio 5 - AFD Minimizado", automataType, AFD_STATES, ALPHABET, "{q0}", List.of("{q2}"), toListTransitionMap(AFD_TRANSITIONS));
        };
    }

    @Override
    public List<AutomataTestResult> testResults() {
        List<AutomataTestResult> results = new ArrayList<>();
        for (int index = 0; index < TEST_INPUTS.size(); index++) {
            String input = TEST_INPUTS.get(index);
            boolean afndAccepted = simulate(AutomataType.AFND, input).accepted();
            boolean afdAccepted = simulate(AutomataType.AFD, input).accepted();
            boolean afdMinAccepted = simulate(AutomataType.AFD_MIN, input).accepted();
            results.add(new AutomataTestResult(
                    index + 1,
                    input,
                    afndAccepted,
                    afdAccepted,
                    afdMinAccepted,
                    afndAccepted == afdAccepted && afdAccepted == afdMinAccepted
            ));
        }
        results.sort(Comparator.comparingInt(AutomataTestResult::index));
        return results;
    }

    private SimulationResult simulateAfnd(String input) {
        Set<String> currentStates = new TreeSet<>(Collections.singleton("q0"));
        List<SimulationStep> steps = new ArrayList<>();

        for (char symbol : input.toCharArray()) {
            Set<String> nextStates = new TreeSet<>();
            for (String currentState : currentStates) {
                nextStates.addAll(AFND_TRANSITIONS.getOrDefault(currentState, Map.of())
                        .getOrDefault(String.valueOf(symbol), List.of()));
            }
            steps.add(new SimulationStep(String.valueOf(symbol), formatStateSet(currentStates), formatStateSet(nextStates)));
            currentStates = nextStates;
        }

        boolean accepted = currentStates.contains("q2");
        return new SimulationResult(accepted, steps, formatStateSet(currentStates), accepted);
    }

    private SimulationResult simulateDeterministic(
            String input,
            String initialState,
            Map<String, Map<String, String>> transitions,
            Set<String> acceptingStates
    ) {
        String currentState = initialState;
        List<SimulationStep> steps = new ArrayList<>();

        for (char symbol : input.toCharArray()) {
            String nextState = transitions.getOrDefault(currentState, Map.of())
                    .getOrDefault(String.valueOf(symbol), "∅");
            steps.add(new SimulationStep(String.valueOf(symbol), currentState, nextState));
            currentState = nextState;
        }

        boolean accepted = acceptingStates.contains(currentState);
        return new SimulationResult(accepted, steps, currentState, accepted);
    }

    private Map<String, Map<String, List<String>>> toListTransitionMap(Map<String, Map<String, String>> transitions) {
        Map<String, Map<String, List<String>>> converted = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, String>> stateEntry : transitions.entrySet()) {
            Map<String, List<String>> stateTransitions = new LinkedHashMap<>();
            for (Map.Entry<String, String> transitionEntry : stateEntry.getValue().entrySet()) {
                stateTransitions.put(transitionEntry.getKey(), List.of(transitionEntry.getValue()));
            }
            converted.put(stateEntry.getKey(), stateTransitions);
        }
        return converted;
    }

    private String formatStateSet(Set<String> states) {
        if (states.isEmpty()) {
            return "∅";
        }
        return "{" + String.join(",", states) + "}";
    }
}