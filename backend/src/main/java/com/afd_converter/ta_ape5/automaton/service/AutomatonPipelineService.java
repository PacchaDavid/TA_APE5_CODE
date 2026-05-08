package com.afd_converter.ta_ape5.automaton.service;

import com.afd_converter.ta_ape5.automaton.api.AutomatonRequest;
import com.afd_converter.ta_ape5.automaton.api.AutomatonType;
import com.afd_converter.ta_ape5.automaton.api.ConvertMinimizeResponse;
import com.afd_converter.ta_ape5.automaton.api.MinimizationResponse;
import com.afd_converter.ta_ape5.automaton.exception.AutomatonConversionException;
import org.springframework.stereotype.Service;

@Service
public class AutomatonPipelineService {
	private final AutomatonConversionService conversionService;
	private final AutomatonMinimizationService minimizationService;

	public AutomatonPipelineService(
			AutomatonConversionService conversionService,
			AutomatonMinimizationService minimizationService
	) {
		this.conversionService = conversionService;
		this.minimizationService = minimizationService;
	}

	/**
	 * Convierte AFND -> AFD y luego minimiza el AFD, retornando pasos de ambos algoritmos.
	 */
	public ConvertMinimizeResponse convertAndMinimize(AutomatonRequest request) {
		var conversion = conversionService.convertFromNfaToDfa(request);

		var dfa = conversion.automaton();
		// Reutilizamos AutomatonRequest como input del servicio de minimización.
		var dfaRequest = new AutomatonRequest(
				AutomatonType.AFD,
				dfa.name(),
				dfa.alphabet(),
				dfa.states(),
				dfa.initialState(),
				dfa.acceptingStates(),
				dfa.transitions().stream()
						.map(t -> new com.afd_converter.ta_ape5.automaton.api.TransitionRequest(t.from(), t.symbol(), t.to()))
						.toList()
		);

		MinimizationResponse minimized;
		try {
			minimized = minimizationService.minimizeDfa(dfaRequest);
		} catch (RuntimeException e) {
			// Si algo falla aquí, es un bug o una inconsistencia en la conversión.
			throw new AutomatonConversionException("No se pudo minimizar el AFD generado: " + e.getMessage());
		}

		return new ConvertMinimizeResponse(
				minimized.automaton(),
				conversion.steps(),
				minimized.steps(),
				minimized.tableSteps()
		);
	}
}
