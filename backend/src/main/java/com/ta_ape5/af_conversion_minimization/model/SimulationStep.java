package com.ta_ape5.af_conversion_minimization.model;

/**
 * Una transicion en el trazado de simulacion.
 *
 * @param symbol    simbolo de entrada consumido
 * @param fromState estado(s) antes de consumir el simbolo
 * @param toState   estado(s) resultante(s) tras la transicion
 */
public record SimulationStep(String symbol, String fromState, String toState) {
}