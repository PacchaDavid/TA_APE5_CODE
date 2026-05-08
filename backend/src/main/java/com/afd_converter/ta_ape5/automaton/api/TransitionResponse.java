package com.afd_converter.ta_ape5.automaton.api;

import java.util.List;

public record TransitionResponse(
		String from,
		String symbol,
		List<String> to
) {
}