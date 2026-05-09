package com.ta_ape5.af_conversion_minimization.model;

/**
 * Payload de solicitud para simulacion.
 *
 * @param exerciseId   identificador de ejercicio (4, 5, o 6)
 * @param automataType tipo de automata (AFND, AFD, AFD_MIN)
 * @param input        cadena de entrada a simular
 */
public record AutomataRequest(int exerciseId, AutomataType automataType, String input) {
}