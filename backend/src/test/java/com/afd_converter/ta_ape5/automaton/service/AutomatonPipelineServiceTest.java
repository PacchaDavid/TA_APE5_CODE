package com.afd_converter.ta_ape5.automaton.service;

import com.afd_converter.ta_ape5.automaton.api.AutomatonRequest;
import com.afd_converter.ta_ape5.automaton.api.AutomatonType;
import com.afd_converter.ta_ape5.automaton.api.TransitionRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AutomatonPipelineServiceTest {
	private final AutomatonPipelineService service = new AutomatonPipelineService(
			new AutomatonConversionService(),
			new AutomatonMinimizationService()
	);

	@Test
	void convertsAndMinimizesAndReturnsBothStepLists() {
		AutomatonRequest request = new AutomatonRequest(
				AutomatonType.AFND,
				"pipeline",
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

		var response = service.convertAndMinimize(request);

		assertThat(response.automaton()).isNotNull();
		assertThat(response.automaton().type()).isEqualTo(AutomatonType.AFD);
		assertThat(response.conversionSteps()).isNotEmpty();
		assertThat(response.minimizationSteps()).isNotEmpty();
		assertThat(response.minimizationTableSteps()).isNotEmpty();
	}
}
