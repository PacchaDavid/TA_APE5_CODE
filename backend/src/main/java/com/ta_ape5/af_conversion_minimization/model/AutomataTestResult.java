package com.ta_ape5.af_conversion_minimization.model;

/**
 * Fila de resultado de pruebas, comparando AFND vs AFD vs AFD_MIN.
 *
 * @param index         indice basado en 1 dentro de la lista
 * @param input         cadena evaluada
 * @param afndAccepted  resultado de aceptacion del AFND
 * @param afdAccepted   resultado de aceptacion del AFD
 * @param afdMinAccepted resultado de aceptacion del AFD minimizado
 * @param equivalent    true cuando los tres resultados coinciden
 */
public record AutomataTestResult(
        int index,
        String input,
        boolean afndAccepted,
        boolean afdAccepted,
        boolean afdMinAccepted,
        boolean equivalent
) {
}