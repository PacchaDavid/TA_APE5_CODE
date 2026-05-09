package com.ta_ape5.af_conversion_minimization.service;

import com.ta_ape5.af_conversion_minimization.model.AutomataType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Exercise4ServiceTests {

    private final Exercise4Service service = new Exercise4Service();

    @Test
    void acceptsAttackPatternInAllVariants() {
        assertThat(service.simulate(AutomataType.AFND, "sar").accepted()).isTrue();
        assertThat(service.simulate(AutomataType.AFD, "sar").accepted()).isTrue();
        assertThat(service.simulate(AutomataType.AFD_MIN, "sar").accepted()).isTrue();
    }

    @Test
    void rejectsIncompletePatternInAllVariants() {
        assertThat(service.simulate(AutomataType.AFND, "sa").accepted()).isFalse();
        assertThat(service.simulate(AutomataType.AFD, "sa").accepted()).isFalse();
        assertThat(service.simulate(AutomataType.AFD_MIN, "sa").accepted()).isFalse();
    }

    @Test
    void exposesExpectedExerciseDefinition() {
        assertThat(service.definition(AutomataType.AFND).exerciseId()).isEqualTo(4);
        assertThat(service.definition(AutomataType.AFND).initialState()).isEqualTo("q0");
        assertThat(service.definition(AutomataType.AFND).acceptingStates()).containsExactly("q3");
    }
}