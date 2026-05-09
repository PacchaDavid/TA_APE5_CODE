package com.ta_ape5.af_conversion_minimization.service;

import com.ta_ape5.af_conversion_minimization.model.AutomataType;
import com.ta_ape5.af_conversion_minimization.model.AutomatonDefinition;
import com.ta_ape5.af_conversion_minimization.model.AutomataTestResult;
import com.ta_ape5.af_conversion_minimization.model.SimulationResult;

import java.util.List;

/**
 * Contrato base que implementa cada ejercicio para exponer sus automatas,
 * simulacion de cadenas y conjunto de pruebas de comparacion.
 */
public interface AutomataService {

    /**
     * Retorna el id del ejercicio manejado por este servicio.
     * Se usa para enrutar solicitudes desde el controlador.
     */
    int exerciseId();

    /**
     * Simula una cadena de entrada en el tipo de automata especificado.
     * Cada implementacion define su propio AFND, AFD y AFD minimizado.
     */
    SimulationResult simulate(AutomataType automataType, String input);

    /**
     * Retorna la definicion del automata para el tipo solicitado.
     * La definicion incluye estados, alfabeto, estado inicial y transiciones.
     */
    AutomatonDefinition definition(AutomataType automataType);

    /**
     * Retorna el conjunto fijo de pruebas con comparacion de aceptacion.
     * Cada resultado indica consistencia entre AFND, AFD y AFD minimizado.
     */
    List<AutomataTestResult> testResults();
}