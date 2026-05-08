package com.afd_converter.ta_ape5.automaton.api;

import java.util.List;

public record AutomatonResponse(
		String name,
		AutomatonType type,
		List<String> alphabet,
		List<String> states,
		String initialState,
		List<String> acceptingStates,
		List<TransitionResponse> transitions
) {
}