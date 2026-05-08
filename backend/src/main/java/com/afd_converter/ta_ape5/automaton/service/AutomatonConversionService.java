package com.afd_converter.ta_ape5.automaton.service;

import com.afd_converter.ta_ape5.automaton.api.AutomatonRequest;
import com.afd_converter.ta_ape5.automaton.api.AutomatonResponse;
import com.afd_converter.ta_ape5.automaton.api.AutomatonType;
import com.afd_converter.ta_ape5.automaton.api.ConversionResponse;
import com.afd_converter.ta_ape5.automaton.api.ConversionStep;
import com.afd_converter.ta_ape5.automaton.api.TransitionRequest;
import com.afd_converter.ta_ape5.automaton.api.TransitionResponse;
import com.afd_converter.ta_ape5.automaton.exception.AutomatonConversionException;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AutomatonConversionService {
	// Símbolo especial que representa las transiciones epsilon (vacías)
	private static final String EPSILON_SYMBOL = "epsilon";
	// Nombre del estado vacío (conjunto vacío) usado como estado sumidero
	private static final String EMPTY_STATE = "EMPTY_SET";

	/**
	 * Convierte un AFND (Autómata Finito No Determinista) a su AFD (Autómata Finito Determinista) equivalente
	 * usando el algoritmo de construcción de subconjuntos. El método retorna también los pasos del algoritmo.
	 * @param request El AFND a convertir en formato JSON
	 * @return Una respuesta con el AFD generado y una lista de pasos del algoritmo
	 */
	public ConversionResponse convertFromNfaToDfa(AutomatonRequest request) {
		// Valida que el AFND tenga una estructura correcta
		validateRequest(request);
		// Extrae el alfabeto del AFND
		Set<String> alphabet = new LinkedHashSet<>(request.alphabet());
		// Construye una estructura de transiciones indexada por estado y símbolo para acceso rápido
		Map<String, Map<String, Set<String>>> transitions = buildTransitions(request.transitions());
		// Detecta si hay transiciones epsilon para ajustar la estrategia de conversión
		boolean hasEpsilonTransitions = request.transitions().stream().anyMatch(this::isEpsilonTransition);
		// Lista de pasos del algoritmo que se devuelve en la respuesta para fines educativos
		List<ConversionStep> steps = new ArrayList<>();
		// Almacena los estados descubiertos del AFD (subconjuntos del AFND)
		List<String> discoveredStates = new ArrayList<>();
		// Almacena los estados de aceptación del AFD
		List<String> acceptingStates = new ArrayList<>();
		// Almacena todas las transiciones del AFD generado
		List<TransitionResponse> deterministicTransitions = new ArrayList<>();
		// Mapeo de nombres de subconjuntos a sus correspondientes conjuntos de estados
		Map<String, Set<String>> subsetByName = new LinkedHashMap<>();
		// Cola de subconjuntos pendientes de procesar (parte del algoritmo BFS)
		Deque<Set<String>> pendingSubsets = new ArrayDeque<>();
		// Conjunto de subconjuntos ya procesados para evitar duplicados
		Set<String> processedSubsets = new LinkedHashSet<>();
		// Estados de aceptación del AFND original para identificar estados de aceptación del AFD
		Set<String> nfaAcceptingStates = new LinkedHashSet<>(request.acceptingStates());

		steps.add(new ConversionStep(1, "Validación", "Se recibió un AFND llamado '" + request.name() + "' con " + request.states().size() + " estados y alfabeto " + alphabet + "."));

		Set<String> startSubset = new LinkedHashSet<>();
		startSubset.add(request.initialState());
		Set<String> startClosure = epsilonClosure(startSubset, transitions);
		String startName = canonicalSubsetName(startClosure);
		subsetByName.put(startName, startClosure);
		pendingSubsets.add(startClosure);
		discoveredStates.add(startName);
		steps.add(new ConversionStep(2, "Estado inicial", "La cerradura epsilon del estado inicial '" + request.initialState() + "' es " + startName + "."));

		if (hasEpsilonTransitions) {
			steps.add(new ConversionStep(3, "Preparación", "El AFND contiene transiciones epsilon, por lo que cada movimiento se cerrará con epsilon-closure."));
		} else {
			steps.add(new ConversionStep(3, "Preparación", "El AFND no contiene transiciones epsilon; la construcción usa solo los símbolos del alfabeto."));
		}

		// ALGORITMO DE CONSTRUCCIÓN DE SUBCONJUNTOS (Subset Construction Algorithm)
		// Recorre iterativamente cada subconjunto hasta procesar todos los descubiertos
		int stepOrder = 4;
		while (!pendingSubsets.isEmpty()) {
			// Extrae el siguiente subconjunto no procesado
			Set<String> currentSubset = pendingSubsets.removeFirst();
			// Genera un nombre canónico para el subconjunto (ej: {q0,q1,q2})
			String currentName = canonicalSubsetName(currentSubset);
			// Marca el subconjunto como procesado; si ya fue procesado, continúa al siguiente
			if (!processedSubsets.add(currentName)) {
				continue;
			}

			// Verifica si el subconjunto contiene al menos un estado de aceptación del AFND
			// Si es así, el estado correspondiente en el AFD es de aceptación
			if (containsAcceptingState(currentSubset, nfaAcceptingStates)) {
				acceptingStates.add(currentName);
			}

			// Para cada símbolo del alfabeto, calcula el estado destino en el AFD
			for (String symbol : alphabet) {
				// Calcula el conjunto de estados alcanzables desde currentSubset con el símbolo
				Set<String> moveResult = move(currentSubset, symbol, transitions);
				// Aplica epsilon-closure para incluir todos los estados alcanzables con transiciones epsilon
				Set<String> targetSubset = epsilonClosure(moveResult, transitions);
				// Genera el nombre canónico del estado destino
				String targetName = canonicalSubsetName(targetSubset);
				// Si es un subconjunto nuevo, lo registra para procesamiento posterior
				if (!subsetByName.containsKey(targetName)) {
					subsetByName.put(targetName, targetSubset);
					discoveredStates.add(targetName);
					pendingSubsets.add(targetSubset);
				}
				// Agrega la transición determinista: (currentName, symbol) -> targetName
				deterministicTransitions.add(new TransitionResponse(currentName, symbol, List.of(targetName)));
				// Registra este paso del algoritmo para la respuesta
				steps.add(new ConversionStep(stepOrder++, "Expansión de subconjunto", "Desde " + currentName + " con símbolo '" + symbol + "' se obtiene " + targetName + "."));
			}
		}

		if (subsetByName.containsKey(EMPTY_STATE)) {
			Set<String> emptySubset = subsetByName.get(EMPTY_STATE);
			for (String symbol : alphabet) {
				deterministicTransitions.add(new TransitionResponse(EMPTY_STATE, symbol, List.of(EMPTY_STATE)));
			}
			if (!processedSubsets.contains(EMPTY_STATE)) {
				discoveredStates.add(EMPTY_STATE);
			}
			steps.add(new ConversionStep(stepOrder++, "Estado sumidero", "Se agregó el estado muerto " + EMPTY_STATE + " para completar el AFD."));
		}

		List<String> orderedStates = orderStates(discoveredStates);
		List<String> orderedAcceptingStates = orderStates(acceptingStates);

		AutomatonResponse automatonResponse = new AutomatonResponse(
				request.name() + "-AFD",
				AutomatonType.AFD,
				new ArrayList<>(alphabet),
				orderedStates,
				startName,
				orderedAcceptingStates,
				mergeTransitions(deterministicTransitions)
		);

		steps.add(new ConversionStep(stepOrder, "Resultado", "Se generó el AFD equivalente con " + orderedStates.size() + " estados alcanzables."));
		return new ConversionResponse(automatonResponse, steps);
	}

	private void validateRequest(AutomatonRequest request) {
		if (request == null) {
			throw new AutomatonConversionException("El cuerpo de la solicitud es obligatorio.");
		}
		if (request.type() == null || request.type() != AutomatonType.AFND) {
			throw new AutomatonConversionException("Esta operación solo acepta un AFND como entrada.");
		}
		Set<String> states = new LinkedHashSet<>(request.states());
		if (!states.contains(request.initialState())) {
			throw new AutomatonConversionException("El estado inicial debe existir en la lista de estados.");
		}
		if (!states.containsAll(request.acceptingStates())) {
			throw new AutomatonConversionException("Todos los estados de aceptación deben existir en la lista de estados.");
		}
		Set<String> alphabet = new LinkedHashSet<>(request.alphabet());
		for (TransitionRequest transition : request.transitions()) {
			if (!states.contains(transition.from())) {
				throw new AutomatonConversionException("La transición parte de un estado inexistente: " + transition.from());
			}
			if (!isEpsilonSymbol(transition.symbol()) && !alphabet.contains(transition.symbol())) {
				throw new AutomatonConversionException("La transición usa un símbolo que no pertenece al alfabeto: " + transition.symbol());
			}
			for (String target : transition.to()) {
				if (!states.contains(target)) {
					throw new AutomatonConversionException("La transición apunta a un estado inexistente: " + target);
				}
			}
		}
	}

	private Map<String, Map<String, Set<String>>> buildTransitions(List<TransitionRequest> transitions) {
		Map<String, Map<String, Set<String>>> byState = new LinkedHashMap<>();
		for (TransitionRequest transition : transitions) {
			byState.computeIfAbsent(transition.from(), key -> new LinkedHashMap<>())
					.computeIfAbsent(normalizeSymbol(transition.symbol()), key -> new LinkedHashSet<>())
					.addAll(new LinkedHashSet<>(transition.to()));
		}
		return byState;
	}

	/**
	 * Calcula el movimiento (move) desde un subconjunto de estados con un símbolo dado.
	 * Retorna la unión de todos los estados alcanzables desde cada estado del subconjunto.
	 */
	private Set<String> move(Set<String> currentSubset, String symbol, Map<String, Map<String, Set<String>>> transitions) {
		// Conjunto que acumula todos los estados alcanzables
		Set<String> result = new LinkedHashSet<>();
		// Para cada estado en el subconjunto
		for (String state : currentSubset) {
			// Obtiene las transiciones definidas para este estado
			Map<String, Set<String>> bySymbol = transitions.getOrDefault(state, Map.of());
			// Obtiene los estados destino para el símbolo especificado
			Set<String> targets = bySymbol.getOrDefault(symbol, Set.of());
			// Agrega todos los destinos al resultado
			result.addAll(targets);
		}
		return result;
	}

	/**
	 * Calcula la cerradura epsilon (epsilon-closure) de un conjunto de estados.
	 * La cerradura epsilon incluye todos los estados alcanzables solo con transiciones epsilon.
	 * Usa una búsqueda en amplitud (BFS) para encontrar todos los estados alcanzables.
	 */
	private Set<String> epsilonClosure(Set<String> states, Map<String, Map<String, Set<String>>> transitions) {
		// Cola para BFS: comienza con los estados iniciales
		Deque<String> queue = new ArrayDeque<>(states);
		// Conjunto que acumula todos los estados en la cerradura
		Set<String> closure = new LinkedHashSet<>(states);
		// Procesa cada estado en la cola
		while (!queue.isEmpty()) {
			String state = queue.removeFirst();
			// Obtiene los estados destino del estado actual mediante transiciones epsilon
			Set<String> epsilonTargets = transitions.getOrDefault(state, Map.of()).getOrDefault(EPSILON_SYMBOL, Set.of());
			// Para cada destino, si no está en la cerradura, lo agrega y lo procesa
			for (String target : epsilonTargets) {
				if (closure.add(target)) {
					// Agrega a la cola para procesar sus transiciones epsilon
					queue.addLast(target);
				}
			}
		}
		// Si la cerradura está vacía, retorna el estado sumidero (EMPTY_STATE)
		return closure.isEmpty() ? Set.of(EMPTY_STATE) : closure;
	}

	private boolean containsAcceptingState(Set<String> currentSubset, Set<String> acceptingStates) {
		for (String state : currentSubset) {
			if (acceptingStates.contains(state)) {
				return true;
			}
		}
		return false;
	}

	private boolean isEpsilonTransition(TransitionRequest transition) {
		return isEpsilonSymbol(transition.symbol());
	}

	private boolean isEpsilonSymbol(String symbol) {
		return symbol != null && ("epsilon".equalsIgnoreCase(symbol) || "eps".equalsIgnoreCase(symbol) || "ε".equals(symbol));
	}

	private String normalizeSymbol(String symbol) {
		return isEpsilonSymbol(symbol) ? EPSILON_SYMBOL : symbol;
	}

	/**
	 * Genera un nombre canónico único para un conjunto de estados.
	 * Ej: {q0, q1, q3} se convierte en "{q0,q1,q3}" ordenado alfabéticamente para garantizar unicidad.
	 */
	private String canonicalSubsetName(Collection<String> subset) {
		// Si el subconjunto es nulo o vacío, retorna el nombre del estado sumidero
		if (subset == null || subset.isEmpty()) {
			return EMPTY_STATE;
		}
		// Genera el nombre ordenando los estados y envolviéndolos entre llaves
		return subset.stream()
				.filter(Objects::nonNull)
				.map(String::trim)
				.filter(value -> !value.isEmpty())
				.sorted(Comparator.naturalOrder())
				.collect(Collectors.joining(",", "{", "}"));
	}

	private List<String> orderStates(Collection<String> states) {
		return states.stream()
				.distinct()
				.sorted(Comparator.naturalOrder())
				.toList();
	}

	private List<TransitionResponse> mergeTransitions(List<TransitionResponse> transitions) {
		Map<String, Map<String, Set<String>>> merged = new LinkedHashMap<>();
		for (TransitionResponse transition : transitions) {
			merged.computeIfAbsent(transition.from(), key -> new LinkedHashMap<>())
					.computeIfAbsent(transition.symbol(), key -> new LinkedHashSet<>())
					.addAll(transition.to());
		}
		List<TransitionResponse> result = new ArrayList<>();
		for (Map.Entry<String, Map<String, Set<String>>> entry : merged.entrySet()) {
			for (Map.Entry<String, Set<String>> symbolEntry : entry.getValue().entrySet()) {
				result.add(new TransitionResponse(entry.getKey(), symbolEntry.getKey(), new ArrayList<>(symbolEntry.getValue())));
			}
		}
		return result;
	}
}