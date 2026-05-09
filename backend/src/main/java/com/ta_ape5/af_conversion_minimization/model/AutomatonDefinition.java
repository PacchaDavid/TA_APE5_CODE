package com.ta_ape5.af_conversion_minimization.model;

import java.util.List;
import java.util.Map;

/**
 * Definicion formal del automata expuesta al frontend.
 *
 * @param exerciseId      identificador del ejercicio
 * @param name            nombre mostrado en la UI
 * @param automataType    tipo de automata
 * @param states          lista de estados
 * @param alphabet        simbolos de entrada
 * @param initialState    estado inicial
 * @param acceptingStates estados de aceptacion
 * @param transitions     tabla de transiciones (estado -> simbolo -> destinos)
 */
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