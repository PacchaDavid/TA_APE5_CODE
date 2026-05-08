package com.afd_converter.ta_ape5.automaton.api;

import java.util.List;

/**
 * Respuesta de minimizacion de AFD.
 */
public record MinimizationResponse(
		AutomatonResponse automaton,
		List<ConversionStep> steps,
		List<MinimizationTableStep> tableSteps
) {
}