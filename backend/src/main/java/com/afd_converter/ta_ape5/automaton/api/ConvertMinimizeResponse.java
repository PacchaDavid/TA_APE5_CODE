package com.afd_converter.ta_ape5.automaton.api;

import java.util.List;

/**
 * Respuesta del flujo combinado: AFND -> AFD -> AFD minimizado.
 */
public record ConvertMinimizeResponse(
		AutomatonResponse automaton,
		List<ConversionStep> conversionSteps,
		List<ConversionStep> minimizationSteps,
		List<MinimizationTableStep> minimizationTableSteps
) {
}
