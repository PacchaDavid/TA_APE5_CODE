package com.ta_ape5.af_conversion_minimization.model;

import java.util.List;

public record SimulationResult(boolean accepted, List<SimulationStep> steps, String finalState, boolean isAccepting) {
}