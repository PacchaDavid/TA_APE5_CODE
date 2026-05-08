package com.afd_converter.ta_ape5.automaton.api;

/**
 * Celda de la tabla de equivalencia de estados.
 * Cada celda representa un par (estadoFila, estadoColumna).
 */
public record MinimizationCell(
		String rowState,
		String columnState,
		boolean marked,
		String reason
) {
}