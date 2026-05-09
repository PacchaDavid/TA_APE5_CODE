package com.ta_ape5.af_conversion_minimization.service;

import com.ta_ape5.af_conversion_minimization.model.AutomataType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Exercise5ServiceTests {

    private final Exercise5Service service = new Exercise5Service();

    @Test
    void acceptsValidTelemetryPackets() {
        assertThat(service.simulate(AutomataType.AFND, "hc").accepted()).isTrue();
        assertThat(service.simulate(AutomataType.AFD, "htmc").accepted()).isTrue();
        assertThat(service.simulate(AutomataType.AFD_MIN, "htttttc").accepted()).isTrue();
    }

    @Test
    void rejectsPacketsWithoutHeaderOrChecksum() {
        assertThat(service.simulate(AutomataType.AFND, "tc").accepted()).isFalse();
        assertThat(service.simulate(AutomataType.AFD, "ht").accepted()).isFalse();
        assertThat(service.simulate(AutomataType.AFD_MIN, "hcc").accepted()).isFalse();
    }

    @Test
    void testResultsKeepEquivalentLanguages() {
        assertThat(service.testResults()).hasSize(20);
        assertThat(service.testResults()).allMatch(result -> result.equivalent());
    }
}