package com.ta_ape5.af_conversion_minimization.service;

import com.ta_ape5.af_conversion_minimization.model.AutomataType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Exercise6ServiceTests {

    private final Exercise6Service service = new Exercise6Service();

    @Test
    void acceptsValidGeneticPatterns() {
        assertThat(service.simulate(AutomataType.AFND, "kgf").accepted()).isTrue();
        assertThat(service.simulate(AutomataType.AFD, "xkgf").accepted()).isTrue();
        assertThat(service.simulate(AutomataType.AFD_MIN, "kgxxf").accepted()).isTrue();
    }

    @Test
    void rejectsInputsWithoutThePattern() {
        assertThat(service.simulate(AutomataType.AFND, "kg").accepted()).isFalse();
        assertThat(service.simulate(AutomataType.AFD, "kf").accepted()).isFalse();
        assertThat(service.simulate(AutomataType.AFD_MIN, "xxxxxf").accepted()).isFalse();
    }

    @Test
    void testResultsAreEquivalentAcrossAllThreeAutomata() {
        assertThat(service.testResults()).hasSize(20);
        assertThat(service.testResults()).allMatch(result -> result.equivalent());
    }
}