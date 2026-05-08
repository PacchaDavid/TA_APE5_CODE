package com.afd_converter.ta_ape5.automaton.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record TransitionRequest(
		@NotBlank String from,
		@NotBlank String symbol,
		@NotEmpty List<@NotBlank String> to
) {
}