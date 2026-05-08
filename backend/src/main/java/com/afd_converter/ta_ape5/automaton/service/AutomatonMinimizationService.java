package com.afd_converter.ta_ape5.automaton.service;

import com.afd_converter.ta_ape5.automaton.api.AutomatonRequest;
import com.afd_converter.ta_ape5.automaton.api.AutomatonResponse;
import com.afd_converter.ta_ape5.automaton.api.AutomatonType;
import com.afd_converter.ta_ape5.automaton.api.ConversionStep;
import com.afd_converter.ta_ape5.automaton.api.MinimizationCell;
import com.afd_converter.ta_ape5.automaton.api.MinimizationResponse;
import com.afd_converter.ta_ape5.automaton.api.MinimizationTableStep;
import com.afd_converter.ta_ape5.automaton.api.TransitionRequest;
import com.afd_converter.ta_ape5.automaton.api.TransitionResponse;
import com.afd_converter.ta_ape5.automaton.exception.AutomatonConversionException;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Servicio que minimiza AFD usando el algoritmo de tabla de equivalencia de estados.
 */
@Service
public class AutomatonMinimizationService {
	private static final String DEAD_STATE = "DEAD_STATE";

	/**
	 * Minimiza un AFD y retorna el automata reducido junto con el paso a paso.
	 */
	public MinimizationResponse minimizeDfa(AutomatonRequest request) {
		validateDfaRequest(request);

		List<ConversionStep> steps = new ArrayList<>();
		List<MinimizationTableStep> tableSteps = new ArrayList<>();

		Set<String> alphabet = new LinkedHashSet<>(request.alphabet());
		Set<String> reachableStates = computeReachableStates(request.initialState(), request.transitions(), alphabet);
		steps.add(new ConversionStep(1, "Estados alcanzables", "Se identificaron " + reachableStates.size() + " estados alcanzables desde el estado inicial."));

		Set<String> workingStates = new LinkedHashSet<>(reachableStates);
		Map<String, Map<String, String>> deterministicTransitions = buildDeterministicTransitions(request.transitions(), workingStates);
		boolean completed = completeTransitions(workingStates, deterministicTransitions, alphabet);
		if (completed) {
			steps.add(new ConversionStep(2, "Completado del AFD", "Se agrego el estado sumidero " + DEAD_STATE + " para completar transiciones faltantes."));
		} else {
			steps.add(new ConversionStep(2, "Completado del AFD", "El AFD ya estaba completo para todo estado y simbolo."));
		}

		List<String> orderedStates = orderStates(workingStates);
		Set<String> acceptingStates = request.acceptingStates().stream()
				.filter(workingStates::contains)
				.collect(Collectors.toCollection(LinkedHashSet::new));

		Map<StatePair, MarkInfo> pairMarks = initializeMarks(orderedStates, acceptingStates);
		tableSteps.add(snapshotTable(0, "Marcado inicial: se marcan pares final/no final.", orderedStates, pairMarks));

		int iteration = 1;
		while (markByTransitionDependency(pairMarks, orderedStates, alphabet, deterministicTransitions, iteration)) {
			tableSteps.add(snapshotTable(iteration, "Iteracion " + iteration + ": marcado por dependencia de transiciones.", orderedStates, pairMarks));
			iteration++;
		}

		steps.add(new ConversionStep(3, "Tabla de equivalencia", "La tabla final tiene " + countUnmarked(pairMarks) + " pares no marcados (equivalentes)."));

		List<Set<String>> classes = buildEquivalenceClasses(orderedStates, pairMarks);
		steps.add(new ConversionStep(4, "Clases de equivalencia", "Se construyeron " + classes.size() + " clases de equivalencia."));

		Map<String, String> stateToClassName = new HashMap<>();
		List<String> minimizedStates = new ArrayList<>();
		for (Set<String> equivalentClass : classes) {
			String className = className(equivalentClass);
			minimizedStates.add(className);
			for (String state : equivalentClass) {
				stateToClassName.put(state, className);
			}
		}

		String minimizedInitial = stateToClassName.get(request.initialState());
		Set<String> minimizedAccepting = new LinkedHashSet<>();
		for (String accepting : acceptingStates) {
			minimizedAccepting.add(stateToClassName.get(accepting));
		}

		List<TransitionResponse> minimizedTransitions = buildMinimizedTransitions(classes, alphabet, deterministicTransitions, stateToClassName);

		AutomatonResponse minimizedAutomaton = new AutomatonResponse(
				request.name() + "-MIN",
				AutomatonType.AFD,
				new ArrayList<>(alphabet),
				orderStates(minimizedStates),
				minimizedInitial,
				orderStates(minimizedAccepting),
				minimizedTransitions
		);

		steps.add(new ConversionStep(5, "Resultado", "Se genero el AFD minimizado con " + minimizedAutomaton.states().size() + " estados."));
		return new MinimizationResponse(minimizedAutomaton, steps, tableSteps);
	}

	/**
	 * Valida que la entrada sea un AFD sin transiciones epsilon y determinista.
	 */
	private void validateDfaRequest(AutomatonRequest request) {
		if (request == null) {
			throw new AutomatonConversionException("El cuerpo de la solicitud es obligatorio.");
		}
		if (request.type() == null || request.type() != AutomatonType.AFD) {
			throw new AutomatonConversionException("Esta operacion solo acepta un AFD como entrada.");
		}
		Set<String> states = new LinkedHashSet<>(request.states());
		if (!states.contains(request.initialState())) {
			throw new AutomatonConversionException("El estado inicial debe existir en la lista de estados.");
		}
		if (!states.containsAll(request.acceptingStates())) {
			throw new AutomatonConversionException("Todos los estados de aceptacion deben existir en la lista de estados.");
		}
		Set<String> alphabet = new LinkedHashSet<>(request.alphabet());
		for (TransitionRequest transition : request.transitions()) {
			if (!states.contains(transition.from())) {
				throw new AutomatonConversionException("La transicion parte de un estado inexistente: " + transition.from());
			}
			if (!alphabet.contains(transition.symbol())) {
				throw new AutomatonConversionException("La transicion usa un simbolo que no pertenece al alfabeto: " + transition.symbol());
			}
			if (transition.to().size() != 1) {
				throw new AutomatonConversionException("Un AFD debe tener exactamente un estado destino por transicion.");
			}
			String target = transition.to().get(0);
			if (!states.contains(target)) {
				throw new AutomatonConversionException("La transicion apunta a un estado inexistente: " + target);
			}
		}
	}

	/**
	 * Obtiene los estados alcanzables mediante BFS desde el estado inicial.
	 */
	private Set<String> computeReachableStates(String initialState, List<TransitionRequest> transitions, Set<String> alphabet) {
		Map<String, Map<String, String>> byState = new LinkedHashMap<>();
		for (TransitionRequest transition : transitions) {
			if (alphabet.contains(transition.symbol())) {
				byState.computeIfAbsent(transition.from(), key -> new LinkedHashMap<>())
						.put(transition.symbol(), transition.to().get(0));
			}
		}

		Set<String> visited = new LinkedHashSet<>();
		Deque<String> queue = new ArrayDeque<>();
		visited.add(initialState);
		queue.add(initialState);

		while (!queue.isEmpty()) {
			String current = queue.removeFirst();
			Map<String, String> nextBySymbol = byState.getOrDefault(current, Map.of());
			for (String symbol : alphabet) {
				String target = nextBySymbol.get(symbol);
				if (target != null && visited.add(target)) {
					queue.addLast(target);
				}
			}
		}

		return visited;
	}

	/**
	 * Construye la funcion de transicion determinista: estado -> simbolo -> estadoDestino.
	 */
	private Map<String, Map<String, String>> buildDeterministicTransitions(List<TransitionRequest> transitions, Set<String> allowedStates) {
		Map<String, Map<String, String>> deterministic = new LinkedHashMap<>();
		for (TransitionRequest transition : transitions) {
			if (!allowedStates.contains(transition.from())) {
				continue;
			}
			String target = transition.to().get(0);
			if (!allowedStates.contains(target)) {
				continue;
			}
			deterministic.computeIfAbsent(transition.from(), key -> new LinkedHashMap<>())
					.put(transition.symbol(), target);
		}
		return deterministic;
	}

	/**
	 * Completa transiciones faltantes agregando estado sumidero cuando sea necesario.
	 */
	private boolean completeTransitions(Set<String> states, Map<String, Map<String, String>> transitions, Set<String> alphabet) {
		boolean needsDeadState = false;
		for (String state : new ArrayList<>(states)) {
			Map<String, String> bySymbol = transitions.computeIfAbsent(state, key -> new LinkedHashMap<>());
			for (String symbol : alphabet) {
				if (!bySymbol.containsKey(symbol)) {
					needsDeadState = true;
					bySymbol.put(symbol, DEAD_STATE);
				}
			}
		}

		if (!needsDeadState) {
			return false;
		}

		states.add(DEAD_STATE);
		Map<String, String> deadTransitions = transitions.computeIfAbsent(DEAD_STATE, key -> new LinkedHashMap<>());
		for (String symbol : alphabet) {
			deadTransitions.put(symbol, DEAD_STATE);
		}
		return true;
	}

	/**
	 * Marca inicialmente los pares donde uno es final y el otro no.
	 */
	private Map<StatePair, MarkInfo> initializeMarks(List<String> orderedStates, Set<String> acceptingStates) {
		Map<StatePair, MarkInfo> marks = new LinkedHashMap<>();
		for (int i = 0; i < orderedStates.size(); i++) {
			for (int j = i + 1; j < orderedStates.size(); j++) {
				String a = orderedStates.get(i);
				String b = orderedStates.get(j);
				boolean finalMismatch = acceptingStates.contains(a) != acceptingStates.contains(b);
				if (finalMismatch) {
					marks.put(StatePair.of(a, b), new MarkInfo(true, "Uno es final y el otro no."));
				} else {
					marks.put(StatePair.of(a, b), new MarkInfo(false, ""));
				}
			}
		}
		return marks;
	}

	/**
	 * Itera la tabla y marca pares distinguibles por dependencia en pares ya marcados.
	 */
	private boolean markByTransitionDependency(
			Map<StatePair, MarkInfo> marks,
			List<String> orderedStates,
			Set<String> alphabet,
			Map<String, Map<String, String>> transitions,
			int iteration
	) {
		boolean changed = false;
		for (int i = 0; i < orderedStates.size(); i++) {
			for (int j = i + 1; j < orderedStates.size(); j++) {
				String a = orderedStates.get(i);
				String b = orderedStates.get(j);
				StatePair currentPair = StatePair.of(a, b);
				MarkInfo currentInfo = marks.get(currentPair);
				if (currentInfo.marked()) {
					continue;
				}

				for (String symbol : alphabet) {
					String targetA = transitions.getOrDefault(a, Map.of()).get(symbol);
					String targetB = transitions.getOrDefault(b, Map.of()).get(symbol);
					if (targetA == null || targetB == null || targetA.equals(targetB)) {
						continue;
					}
					StatePair targetPair = StatePair.of(targetA, targetB);
					MarkInfo targetInfo = marks.get(targetPair);
					if (targetInfo != null && targetInfo.marked()) {
						marks.put(currentPair, new MarkInfo(true, "Iteracion " + iteration + ": con simbolo '" + symbol + "' transita a " + targetPair.label() + ", que ya esta marcado."));
						changed = true;
						break;
					}
				}
			}
		}
		return changed;
	}

	/**
	 * Crea un snapshot de la tabla para ser renderizado en frontend.
	 */
	private MinimizationTableStep snapshotTable(int iteration, String description, List<String> orderedStates, Map<StatePair, MarkInfo> marks) {
		List<MinimizationCell> cells = new ArrayList<>();
		for (int i = 0; i < orderedStates.size(); i++) {
			for (int j = i + 1; j < orderedStates.size(); j++) {
				String row = orderedStates.get(j);
				String col = orderedStates.get(i);
				StatePair pair = StatePair.of(row, col);
				MarkInfo info = marks.get(pair);
				cells.add(new MinimizationCell(row, col, info != null && info.marked(), info == null ? "" : info.reason()));
			}
		}
		return new MinimizationTableStep(iteration, description, cells);
	}

	/**
	 * Construye clases de equivalencia uniendo pares no marcados.
	 */
	private List<Set<String>> buildEquivalenceClasses(List<String> states, Map<StatePair, MarkInfo> marks) {
		Map<String, String> parent = new LinkedHashMap<>();
		for (String state : states) {
			parent.put(state, state);
		}

		for (Map.Entry<StatePair, MarkInfo> entry : marks.entrySet()) {
			if (!entry.getValue().marked()) {
				StatePair pair = entry.getKey();
				union(parent, pair.a(), pair.b());
			}
		}

		Map<String, Set<String>> classes = new LinkedHashMap<>();
		for (String state : states) {
			String root = find(parent, state);
			classes.computeIfAbsent(root, key -> new LinkedHashSet<>()).add(state);
		}
		return new ArrayList<>(classes.values());
	}

	/**
	 * Construye transiciones del automata minimizado.
	 */
	private List<TransitionResponse> buildMinimizedTransitions(
			List<Set<String>> classes,
			Set<String> alphabet,
			Map<String, Map<String, String>> transitions,
			Map<String, String> stateToClassName
	) {
		List<TransitionResponse> result = new ArrayList<>();
		for (Set<String> equivalentClass : classes) {
			String representative = equivalentClass.stream().sorted().findFirst().orElseThrow();
			String from = stateToClassName.get(representative);
			Map<String, String> bySymbol = transitions.getOrDefault(representative, Map.of());
			for (String symbol : alphabet) {
				String target = bySymbol.get(symbol);
				String minimizedTarget = stateToClassName.get(target);
				result.add(new TransitionResponse(from, symbol, List.of(minimizedTarget)));
			}
		}
		return mergeTransitions(result);
	}

	private int countUnmarked(Map<StatePair, MarkInfo> marks) {
		return (int) marks.values().stream().filter(info -> !info.marked()).count();
	}

	private String className(Collection<String> states) {
		return states.stream().sorted().collect(Collectors.joining(",", "{", "}"));
	}

	private List<String> orderStates(Collection<String> states) {
		return states.stream().distinct().sorted(Comparator.naturalOrder()).toList();
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

	private String find(Map<String, String> parent, String state) {
		String root = parent.get(state);
		if (root.equals(state)) {
			return root;
		}
		String compressed = find(parent, root);
		parent.put(state, compressed);
		return compressed;
	}

	private void union(Map<String, String> parent, String a, String b) {
		String rootA = find(parent, a);
		String rootB = find(parent, b);
		if (!rootA.equals(rootB)) {
			if (rootA.compareTo(rootB) < 0) {
				parent.put(rootB, rootA);
			} else {
				parent.put(rootA, rootB);
			}
		}
	}

	private record MarkInfo(boolean marked, String reason) {
	}

	private record StatePair(String a, String b) {
		private static StatePair of(String left, String right) {
			if (left.compareTo(right) <= 0) {
				return new StatePair(left, right);
			}
			return new StatePair(right, left);
		}

		private String label() {
			return "(" + a + "," + b + ")";
		}
	}
}