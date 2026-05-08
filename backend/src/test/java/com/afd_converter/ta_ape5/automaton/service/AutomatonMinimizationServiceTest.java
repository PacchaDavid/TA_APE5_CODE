package com.afd_converter.ta_ape5.automaton.service;

import com.afd_converter.ta_ape5.automaton.api.AutomatonRequest;
import com.afd_converter.ta_ape5.automaton.api.AutomatonType;
import com.afd_converter.ta_ape5.automaton.api.MinimizationResponse;
import com.afd_converter.ta_ape5.automaton.api.TransitionRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AutomatonMinimizationServiceTest {
	private final AutomatonMinimizationService service = new AutomatonMinimizationService();

	@Test
	void minimizesDfaAndReturnsTableSteps() {
		// AFD donde q1 y q2 son equivalentes y pueden fusionarse.
		AutomatonRequest request = new AutomatonRequest(
				AutomatonType.AFD,
				"dfa-min",
				List.of("0", "1"),
				List.of("q0", "q1", "q2", "q3"),
				"q0",
				List.of("q3"),
				List.of(
						new TransitionRequest("q0", "0", List.of("q1")),
						new TransitionRequest("q0", "1", List.of("q2")),
						new TransitionRequest("q1", "0", List.of("q1")),
						new TransitionRequest("q1", "1", List.of("q3")),
						new TransitionRequest("q2", "0", List.of("q1")),
						new TransitionRequest("q2", "1", List.of("q3")),
						new TransitionRequest("q3", "0", List.of("q3")),
						new TransitionRequest("q3", "1", List.of("q3"))
				)
		);

		MinimizationResponse response = service.minimizeDfa(request);

		assertThat(response.automaton().type()).isEqualTo(AutomatonType.AFD);
		assertThat(response.automaton().states().size()).isLessThan(request.states().size());
		assertThat(response.tableSteps()).isNotEmpty();
		assertThat(response.steps()).isNotEmpty();
	}

	@Test
	void rejectsNonDfaInputForMinimization() {
		AutomatonRequest request = new AutomatonRequest(
				AutomatonType.AFND,
				"invalid",
				List.of("a"),
				List.of("q0"),
				"q0",
				List.of("q0"),
				List.of(new TransitionRequest("q0", "a", List.of("q0")))
		);

		assertThatThrownBy(() -> service.minimizeDfa(request))
				.hasMessageContaining("solo acepta un AFD");
	}
}