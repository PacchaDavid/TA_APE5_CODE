package com.afd_converter.ta_ape5.automaton.api;

import java.util.List;

public record ConversionResponse(
		AutomatonResponse automaton,
		List<ConversionStep> steps
) {
}