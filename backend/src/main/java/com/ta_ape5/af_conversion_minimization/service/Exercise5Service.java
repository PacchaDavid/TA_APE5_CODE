package com.ta_ape5.af_conversion_minimization.service;

import com.ta_ape5.af_conversion_minimization.model.AutomataTestResult;
import com.ta_ape5.af_conversion_minimization.model.AutomataType;
import com.ta_ape5.af_conversion_minimization.model.AutomatonDefinition;
import com.ta_ape5.af_conversion_minimization.model.SimulationResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Ejercicio 5: validacion de paquete IoT (HDR -> (TEMP|HUM)* -> CRC).
 * El AFND admite multiples lecturas de sensores antes del cierre con CRC.
 */
@Service
public class Exercise5Service implements AutomataService {

        /** Identificador del ejercicio. */
    private static final int EXERCISE_ID = 5;

    /** Estados del AFND. */
    private static final List<String> AFND_STATES = List.of("q0", "q1", "q2");
    /** Simbolos de entrada: h=HDR, t=TEMP, m=HUM, c=CRC. */
    private static final List<String> ALPHABET = List.of("h", "t", "m", "c");

    /** Tabla de transiciones del AFND. */
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

    private static final AutomatonDefinition AFND_DEFINITION = new AutomatonDefinition(
            EXERCISE_ID,
            "Ejercicio 5 - AFND",
            AutomataType.AFND,
            AFND_STATES,
            ALPHABET,
            "q0",
            List.of("q2"),
            AFND_TRANSITIONS
    );

    private static final AutomataAlgorithms.DeterministicAutomaton AFD_AUTOMATON =
            AutomataAlgorithms.afndToAfd(AFND_DEFINITION);

    private static final AutomataAlgorithms.DeterministicAutomaton AFD_MIN_AUTOMATON =
            AutomataAlgorithms.minimizeAfd(AFD_AUTOMATON);

    private static final AutomatonDefinition AFD_DEFINITION = AutomataAlgorithms.toDefinition(
            AFD_AUTOMATON,
            EXERCISE_ID,
            "Ejercicio 5 - AFD",
            AutomataType.AFD
    );

    private static final AutomatonDefinition AFD_MIN_DEFINITION = AutomataAlgorithms.toDefinition(
            AFD_MIN_AUTOMATON,
            EXERCISE_ID,
            "Ejercicio 5 - AFD Minimizado",
            AutomataType.AFD_MIN
    );

    /** Conjunto fijo de entradas usado en la tabla de comparacion. */
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

    /** {@inheritDoc} */
    @Override
    public int exerciseId() {
        return EXERCISE_ID;
    }

    /** {@inheritDoc} */
    @Override
    public SimulationResult simulate(AutomataType automataType, String input) {
        String safeInput = input == null ? "" : input;
        return switch (automataType) {
            case AFND -> AutomataAlgorithms.simulateAfnd(AFND_DEFINITION, safeInput);
            case AFD -> AutomataAlgorithms.simulateAfd(AFD_AUTOMATON, safeInput);
            case AFD_MIN -> AutomataAlgorithms.simulateAfd(AFD_MIN_AUTOMATON, safeInput);
        };
    }

    /** {@inheritDoc} */
    @Override
    public AutomatonDefinition definition(AutomataType automataType) {
        return switch (automataType) {
            case AFND -> AFND_DEFINITION;
            case AFD -> AFD_DEFINITION;
            case AFD_MIN -> AFD_MIN_DEFINITION;
        };
    }

    /** {@inheritDoc} */
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

}