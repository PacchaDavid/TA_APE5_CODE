package com.ta_ape5.af_conversion_minimization;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada de Spring Boot para la API de conversion/minimizacion de automatas.
 */
@SpringBootApplication
public class AfConversionMinimizationApplication {

	/**
	 * Inicializa el contexto de Spring y levanta el servidor embebido.
	 */
	public static void main(String[] args) {
		SpringApplication.run(AfConversionMinimizationApplication.class, args);
	}

}
