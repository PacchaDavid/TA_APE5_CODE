package com.ta_ape5.af_conversion_minimization.controller;

import com.ta_ape5.af_conversion_minimization.model.AutomataRequest;
import com.ta_ape5.af_conversion_minimization.model.AutomataType;
import com.ta_ape5.af_conversion_minimization.model.AutomatonDefinition;
import com.ta_ape5.af_conversion_minimization.model.AutomataTestResult;
import com.ta_ape5.af_conversion_minimization.model.SimulationResult;
import com.ta_ape5.af_conversion_minimization.service.AutomataService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Controlador REST que expone definiciones, simulaciones y datos de prueba.
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/automata")
public class AutomataController {

    private final List<AutomataService> automataServices;

    /**
     * Inyecta los servicios de ejercicios para despachar por id de ejercicio.
     */
    public AutomataController(List<AutomataService> automataServices) {
        this.automataServices = automataServices;
    }

    /**
     * Simula una cadena sobre el tipo de automata solicitado.
     */
    @PostMapping("/simulate")
    public SimulationResult simulate(@RequestBody AutomataRequest request) {
        AutomataService service = findService(request.exerciseId());
        return service.simulate(request.automataType(), request.input());
    }

    /**
     * Retorna la definicion formal para un ejercicio y tipo de automata.
     */
    @GetMapping("/definition/{exerciseId}")
    public AutomatonDefinition definition(
            @PathVariable int exerciseId,
            @RequestParam(defaultValue = "AFND") AutomataType automataType
    ) {
        AutomataService service = findService(exerciseId);
        return service.definition(automataType);
    }

    /**
     * Retorna el conjunto fijo de pruebas para comparar AFND/AFD/AFD_MIN.
     */
    @GetMapping("/test/{exerciseId}")
    public List<AutomataTestResult> testResults(@PathVariable int exerciseId) {
        AutomataService service = findService(exerciseId);
        return service.testResults();
    }

    /**
     * Ubica el servicio que implementa un id de ejercicio especifico.
     */
    private AutomataService findService(int exerciseId) {
        return automataServices.stream()
                .filter(service -> service.exerciseId() == exerciseId)
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Exercise not implemented yet"));
    }
}