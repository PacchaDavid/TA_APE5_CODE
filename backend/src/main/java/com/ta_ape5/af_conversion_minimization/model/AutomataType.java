package com.ta_ape5.af_conversion_minimization.model;

/**
 * Variantes de automata soportadas por ejercicio.
 */
public enum AutomataType {
    /** Automata finito no determinista. */
    AFND,
    /** Automata finito determinista equivalente al AFND. */
    AFD,
    /** Automata finito determinista minimizado. */
    AFD_MIN
}