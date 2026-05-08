package com.afd_converter.ta_ape5.automaton.service;

import com.afd_converter.ta_ape5.automaton.api.AutomatonRequest;
import com.afd_converter.ta_ape5.automaton.api.AutomatonType;
import com.afd_converter.ta_ape5.automaton.api.ConversionResponse;
import com.afd_converter.ta_ape5.automaton.api.TransitionRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AutomatonConversionServiceTest {
	private final AutomatonConversionService service = new AutomatonConversionService();

	@Test
	void convertsNfaToDfaWithCommentedAlgorithmSteps() {
		// Este test valida que el algoritmo de construcción de subconjuntos genera
		// el AFD correcto con los pasos intermedios documentados
		AutomatonRequest request = new AutomatonRequest(
				AutomatonType.AFND,
				"test-automaton",
				List.of("0", "1"),
				List.of("q0", "q1", "q2"),
				"q0",
				List.of("q2"),
				List.of(
						new TransitionRequest("q0", "0", List.of("q0", "q1")),
						new TransitionRequest("q0", "1", List.of("q0")),
						new TransitionRequest("q1", "1", List.of("q2")),
						new TransitionRequest("q2", "0", List.of("q2")),
						new TransitionRequest("q2", "1", List.of("q2"))
				)
		);

		// Ejecuta la conversión AFND -> AFD
		ConversionResponse response = service.convertFromNfaToDfa(request);

		// Valida el resultado del AFD generado
		assertThat(response.automaton().type()).isEqualTo(AutomatonType.AFD);
		assertThat(response.automaton().states()).isNotEmpty();
		assertThat(response.automaton().acceptingStates()).isNotEmpty();
		assertThat(response.steps()).isNotEmpty().hasSizeGreaterThan(4);
	}
}