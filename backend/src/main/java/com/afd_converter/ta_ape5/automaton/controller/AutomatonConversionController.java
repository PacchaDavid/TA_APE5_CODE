package com.afd_converter.ta_ape5.automaton.controller;

import com.afd_converter.ta_ape5.automaton.api.AutomatonRequest;
import com.afd_converter.ta_ape5.automaton.api.ConvertMinimizeResponse;
import com.afd_converter.ta_ape5.automaton.api.ConversionResponse;
import com.afd_converter.ta_ape5.automaton.api.MinimizationResponse;
import com.afd_converter.ta_ape5.automaton.service.AutomatonConversionService;
import com.afd_converter.ta_ape5.automaton.service.AutomatonMinimizationService;
import com.afd_converter.ta_ape5.automaton.service.AutomatonPipelineService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST que expone los endpoints para la conversion de automatas finitos.
 * Recibe solicitudes AFND y devuelve la conversion a AFD con los pasos del algoritmo.
 */
@RestController
@RequestMapping("/api/automata")
public class AutomatonConversionController {
	// Inyeccion de dependencia del servicio de conversion
	private final AutomatonConversionService conversionService;
	// Inyeccion de dependencia del servicio de minimizacion
	private final AutomatonMinimizationService minimizationService;
	// Inyeccion de dependencia del flujo combinado
	private final AutomatonPipelineService pipelineService;

	/**
	 * Constructor que inyecta el servicio de conversion.
	 */
	public AutomatonConversionController(
			AutomatonConversionService conversionService,
			AutomatonMinimizationService minimizationService,
			AutomatonPipelineService pipelineService
	) {
		this.conversionService = conversionService;
		this.minimizationService = minimizationService;
		this.pipelineService = pipelineService;
	}

	/**
	 * Endpoint POST que convierte un AFND a AFD.
	 * URL: POST /api/automata/convert
	 * @param request El cuerpo de la solicitud con el AFND a convertir
	 * @return Respuesta HTTP 200 con el AFD generado y los pasos del algoritmo
	 */
	@PostMapping("/convert")
	public ResponseEntity<ConversionResponse> convert(@Valid @RequestBody AutomatonRequest request) {
		// Invoca el servicio para realizar la conversion y devuelve el resultado
		return ResponseEntity.ok(conversionService.convertFromNfaToDfa(request));
	}

	/**
	 * Endpoint POST que minimiza un AFD.
	 * URL: POST /api/automata/minimize
	 * @param request El cuerpo de la solicitud con el AFD a minimizar
	 * @return Respuesta HTTP 200 con el AFD minimizado y los pasos del algoritmo
	 */
	@PostMapping("/minimize")
	public ResponseEntity<MinimizationResponse> minimize(@Valid @RequestBody AutomatonRequest request) {
		return ResponseEntity.ok(minimizationService.minimizeDfa(request));
	}

	/**
	 * Endpoint POST que convierte un AFND a AFD y luego minimiza el AFD resultante.
	 * URL: POST /api/automata/convert-minimize
	 * @param request El cuerpo de la solicitud con el AFND de entrada
	 * @return Respuesta HTTP 200 con el AFD minimizado y pasos de ambos algoritmos
	 */
	@PostMapping("/convert-minimize")
	public ResponseEntity<ConvertMinimizeResponse> convertAndMinimize(@Valid @RequestBody AutomatonRequest request) {
		return ResponseEntity.ok(pipelineService.convertAndMinimize(request));
	}
}
