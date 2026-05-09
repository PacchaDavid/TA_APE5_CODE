package com.ta_ape5.af_conversion_minimization.service;

import com.ta_ape5.af_conversion_minimization.model.AutomataType;
import com.ta_ape5.af_conversion_minimization.model.AutomatonDefinition;
import com.ta_ape5.af_conversion_minimization.model.AutomataTestResult;
import com.ta_ape5.af_conversion_minimization.model.SimulationResult;

import java.util.List;

public interface AutomataService {

    int exerciseId();

    SimulationResult simulate(AutomataType automataType, String input);

    AutomatonDefinition definition(AutomataType automataType);

    List<AutomataTestResult> testResults();
}