package com.afd_converter.ta_ape5.automaton.api;

import java.util.List;

/**
 * Paso de la tabla de equivalencia para visualizacion en frontend.
 */
public record MinimizationTableStep(
		int iteration,
		String description,
		List<MinimizationCell> cells
) {
}