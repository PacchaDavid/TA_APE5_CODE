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
public class Exercise6Service implements AutomataService {

    private static final int EXERCISE_ID = 6;

    private static final List<String> AFND_STATES = List.of("q0", "q1", "q2", "q3");
    private static final List<String> AFD_STATES = List.of("{q0}", "{q0,q1}", "{q0,q2}", "{q0,q1,q2}", "{q0,q2,q3}");
    private static final List<String> AFD_MIN_STATES = List.of("{q0}", "{q0,q1}", "{q0,q1/q2}", "{q0,q2,q3}");
    private static final List<String> ALPHABET = List.of("k", "g", "f", "x");

    private static final Map<String, Map<String, List<String>>> AFND_TRANSITIONS = Map.of(
            "q0", Map.of(
                    "k", List.of("q0", "q1"),
                    "g", List.of("q0"),
                    "f", List.of("q0"),
                    "x", List.of("q0")
            ),
            "q1", Map.of(
                    "g", List.of("q2")
            ),
            "q2", Map.of(
                    "k", List.of("q2"),
                    "g", List.of("q2"),
                    "f", List.of("q2", "q3"),
                    "x", List.of("q2")
            ),
            "q3", Map.of()
    );

    private static final Map<String, Map<String, String>> AFD_TRANSITIONS = Map.of(
            "{q0}", Map.of(
                    "k", "{q0,q1}",
                    "g", "{q0}",
                    "f", "{q0}",
                    "x", "{q0}"
            ),
            "{q0,q1}", Map.of(
                    "k", "{q0,q1}",
                    "g", "{q0,q2}",
                    "f", "{q0}",
                    "x", "{q0}"
            ),
            "{q0,q2}", Map.of(
                    "k", "{q0,q1,q2}",
                    "g", "{q0,q2}",
                    "f", "{q0,q2,q3}",
                    "x", "{q0,q2}"
            ),
            "{q0,q1,q2}", Map.of(
                    "k", "{q0,q1,q2}",
                    "g", "{q0,q2}",
                    "f", "{q0,q2,q3}",
                    "x", "{q0,q2}"
            ),
            "{q0,q2,q3}", Map.of(
                    "k", "{q0,q1,q2}",
                    "g", "{q0,q2}",
                    "f", "{q0,q2,q3}",
                    "x", "{q0,q2}"
            )
    );

    private static final Map<String, Map<String, String>> AFD_MIN_TRANSITIONS = Map.of(
            "{q0}", Map.of(
                    "k", "{q0,q1}",
                    "g", "{q0}",
                    "f", "{q0}",
                    "x", "{q0}"
            ),
            "{q0,q1}", Map.of(
                    "k", "{q0,q1}",
                    "g", "{q0,q1/q2}",
                    "f", "{q0}",
                    "x", "{q0}"
            ),
            "{q0,q1/q2}", Map.of(
                    "k", "{q0,q1/q2}",
                    "g", "{q0,q1/q2}",
                    "f", "{q0,q2,q3}",
                    "x", "{q0,q1/q2}"
            ),
            "{q0,q2,q3}", Map.of(
                    "k", "{q0,q1/q2}",
                    "g", "{q0,q1/q2}",
                    "f", "{q0,q2,q3}",
                    "x", "{q0,q1/q2}"
            )
    );

    private static final List<String> TEST_INPUTS = List.of(
            "",
            "kgf",
            "kgxf",
            "kgxxf",
            "kg",
            "gf",
            "kf",
            "xkgf",
            "xxxkgf",
            "kgfxxx",
            "f",
            "k",
            "kgkgf",
            "kgfkgf",
            "xxxxxf",
            "kgxxxf",
            "kgfgkgf",
            "kkgf",
            "kggf",
            "kgkf"
    );

    @Override
    public int exerciseId() {
        return EXERCISE_ID;
    }

    @Override
    public SimulationResult simulate(AutomataType automataType, String input) {
        return switch (automataType) {
            case AFND -> simulateAfnd(input == null ? "" : input);
            case AFD -> simulateDeterministic(input == null ? "" : input, "{q0}", AFD_TRANSITIONS, Set.of("{q0,q2,q3}"));
            case AFD_MIN -> simulateDeterministic(input == null ? "" : input, "{q0}", AFD_MIN_TRANSITIONS, Set.of("{q0,q2,q3}"));
        };
    }

    @Override
    public AutomatonDefinition definition(AutomataType automataType) {
        return switch (automataType) {
            case AFND -> new AutomatonDefinition(EXERCISE_ID, "Ejercicio 6 - AFND", automataType, AFND_STATES, ALPHABET, "q0", List.of("q3"), AFND_TRANSITIONS);
            case AFD -> new AutomatonDefinition(EXERCISE_ID, "Ejercicio 6 - AFD", automataType, AFD_STATES, ALPHABET, "{q0}", List.of("{q0,q2,q3}"), toListTransitionMap(AFD_TRANSITIONS));
            case AFD_MIN -> new AutomatonDefinition(EXERCISE_ID, "Ejercicio 6 - AFD Minimizado", automataType, AFD_MIN_STATES, ALPHABET, "{q0}", List.of("{q0,q2,q3}"), toListTransitionMap(AFD_MIN_TRANSITIONS));
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

        boolean accepted = currentStates.contains("q3");
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
                    .getOrDefault(String.valueOf(symbol), currentState);
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