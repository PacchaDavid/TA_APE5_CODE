package com.afd_converter.ta_ape5.automaton.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AutomatonRequest(
		AutomatonType type,
		@NotBlank String name,
		@NotEmpty List<@NotBlank String> alphabet,
		@NotEmpty List<@NotBlank String> states,
		@NotBlank String initialState,
		@NotEmpty List<@NotBlank String> acceptingStates,
		@NotEmpty List<@Valid TransitionRequest> transitions
) {
}