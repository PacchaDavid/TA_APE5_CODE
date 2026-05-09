package com.ta_ape5.af_conversion_minimization.service;

import com.ta_ape5.af_conversion_minimization.model.AutomataType;
import com.ta_ape5.af_conversion_minimization.model.AutomatonDefinition;
import com.ta_ape5.af_conversion_minimization.model.AutomataTestResult;
import com.ta_ape5.af_conversion_minimization.model.SimulationResult;

import java.util.List;

/**
 * Contrato que implementa cada ejercicio para exponer su automata, simulacion y pruebas.
 */
public interface AutomataService {

    /**
     * Retorna el id del ejercicio manejado por este servicio.
     */
    int exerciseId();

    /**
     * Simula una cadena de entrada en el tipo de automata especificado.
     */
    SimulationResult simulate(AutomataType automataType, String input);

    /**
     * Retorna la definicion del automata para el tipo solicitado.
     */
    AutomatonDefinition definition(AutomataType automataType);

    /**
     * Retorna el conjunto fijo de pruebas con comparacion de aceptacion.
     */
    List<AutomataTestResult> testResults();
}