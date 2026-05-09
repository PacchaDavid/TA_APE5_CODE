package com.ta_ape5.af_conversion_minimization.model;

import java.util.List;

/**
 * Resultado de simular una cadena sobre un automata.
 *
 * @param accepted   true si el estado final es de aceptacion
 * @param steps      trazado paso a paso de la simulacion
 * @param finalState estado final tras consumir la entrada
 * @param isAccepting bandera duplicada usada por la UI para el estado
 */
public record SimulationResult(boolean accepted, List<SimulationStep> steps, String finalState, boolean isAccepting) {
}