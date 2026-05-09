package com.ta_ape5.af_conversion_minimization.service;

import com.ta_ape5.af_conversion_minimization.model.AutomataType;
import com.ta_ape5.af_conversion_minimization.model.AutomatonDefinition;
import com.ta_ape5.af_conversion_minimization.model.AutomataTestResult;
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
public class Exercise4Service implements AutomataService {

    private static final int EXERCISE_ID = 4;

    private static final List<String> STATES = List.of("q0", "q1", "q2", "q3", "qE");
    private static final List<String> ALPHABET = List.of("s", "a", "r", "o");

    private static final Map<String, Map<String, List<String>>> AFND_TRANSITIONS = Map.of(
            "q0", Map.of(
                    "s", List.of("q0", "q1"),
                    "a", List.of("q0"),
                    "r", List.of("q0"),
                    "o", List.of("q0")
            ),
            "q1", Map.of(
                    "a", List.of("q2"),
                    "o", List.of("qE")
            ),
            "q2", Map.of(
                    "a", List.of("q2"),
                    "r", List.of("q3"),
                    "o", List.of("qE")
            ),
            "q3", Map.of(),
            "qE", Map.of()
    );

    private static final Map<String, Map<String, String>> AFD_TRANSITIONS = Map.of(
            "{q0}", Map.of(
                    "s", "{q0,q1}",
                    "a", "{q0}",
                    "r", "{q0}",
                    "o", "{q0}"
            ),
            "{q0,q1}", Map.of(
                    "s", "{q0,q1}",
                    "a", "{q0,q2}",
                    "r", "{q0}",
                    "o", "{q0,qE}"
            ),
            "{q0,q2}", Map.of(
                    "s", "{q0,q1}",
                    "a", "{q0,q2}",
                    "r", "{q0,q3}",
                    "o", "{q0,qE}"
            ),
            "{q0,qE}", Map.of(
                    "s", "{q0,q1}",
                    "a", "{q0}",
                    "r", "{q0}",
                    "o", "{q0}"
            ),
            "{q0,q3}", Map.of(
                    "s", "{q0,q1}",
                    "a", "{q0}",
                    "r", "{q0}",
                    "o", "{q0}"
            )
    );

    private static final Map<String, Map<String, String>> AFD_MIN_TRANSITIONS = Map.of(
            "{q0}", Map.of(
                    "s", "{q0,q1}",
                    "a", "{q0}",
                    "r", "{q0}",
                    "o", "{q0}"
            ),
            "{q0,q1}", Map.of(
                    "s", "{q0,q1}",
                    "a", "{q0,q2}",
                    "r", "{q0}",
                    "o", "{q0}"
            ),
            "{q0,q2}", Map.of(
                    "s", "{q0,q1}",
                    "a", "{q0,q2}",
                    "r", "{q0,q3}",
                    "o", "{q0}"
            ),
            "{q0,q3}", Map.of(
                    "s", "{q0,q1}",
                    "a", "{q0}",
                    "r", "{q0}",
                    "o", "{q0}"
            )
    );

    private static final List<String> TEST_INPUTS = List.of(
            "",
            "s",
            "sa",
            "sar",
            "saar",
            "saaar",
            "r",
            "a",
            "so",
            "sao",
            "o",
            "ssaar",
            "osaar",
            "sarsar",
            "saor",
            "srr",
            "ooosar",
            "saaaar",
            "saaarsar",
            "saaraao"
    );

    @Override
    public int exerciseId() {
        return EXERCISE_ID;
    }

    @Override
    public SimulationResult simulate(AutomataType automataType, String input) {
        return switch (automataType) {
            case AFND -> simulateAfnd(input == null ? "" : input);
            case AFD -> simulateDeterministic(input == null ? "" : input, "{q0}", AFD_TRANSITIONS, Set.of("{q0,q3}"));
            case AFD_MIN -> simulateDeterministic(input == null ? "" : input, "{q0}", AFD_MIN_TRANSITIONS, Set.of("{q0,q3}"));
        };
    }

    @Override
    public AutomatonDefinition definition(AutomataType automataType) {
        return switch (automataType) {
            case AFND -> new AutomatonDefinition(EXERCISE_ID, "Ejercicio 4 - AFND", automataType, STATES, ALPHABET, "q0", List.of("q3"), AFND_TRANSITIONS);
            case AFD -> new AutomatonDefinition(EXERCISE_ID, "Ejercicio 4 - AFD", automataType, List.of("{q0}", "{q0,q1}", "{q0,q2}", "{q0,qE}", "{q0,q3}"), ALPHABET, "{q0}", List.of("{q0,q3}"), toListTransitionMap(AFD_TRANSITIONS));
            case AFD_MIN -> new AutomatonDefinition(EXERCISE_ID, "Ejercicio 4 - AFD Minimizado", automataType, List.of("{q0}", "{q0,q1}", "{q0,q2}", "{q0,q3}"), ALPHABET, "{q0}", List.of("{q0,q3}"), toListTransitionMap(AFD_MIN_TRANSITIONS));
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