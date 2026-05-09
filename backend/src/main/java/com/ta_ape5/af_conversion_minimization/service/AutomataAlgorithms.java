package com.ta_ape5.af_conversion_minimization.service;

import com.ta_ape5.af_conversion_minimization.model.AutomataType;
import com.ta_ape5.af_conversion_minimization.model.AutomatonDefinition;
import com.ta_ape5.af_conversion_minimization.model.SimulationResult;
import com.ta_ape5.af_conversion_minimization.model.SimulationStep;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Algoritmos de conversion, simulacion y minimizacion para automatas finitos.
 * Centraliza la conversion AFND->AFD (construccion de subconjuntos),
 * minimizacion de AFD (tabla de distincion) y adaptadores de definicion.
 */
public final class AutomataAlgorithms {

    // Estado trampa canonico para completar transiciones deterministas.
    private static final String TRAP_STATE = "∅";

    private AutomataAlgorithms() {
    }

    /**
     * Estructura inmutable para representar un AFD y sus metadatos.
     * Las transiciones son deterministas y se indexan por estado y simbolo.
     */
    public record DeterministicAutomaton(
            List<String> states,
            List<String> alphabet,
            String initialState,
            List<String> acceptingStates,
            Map<String, Map<String, String>> transitions
    ) {
    }

    /**
     * Convierte un AFND a un AFD mediante construccion de subconjuntos.
     * Genera solo los superestados alcanzables desde el estado inicial.
     */
    public static DeterministicAutomaton afndToAfd(AutomatonDefinition afnd) {
        Set<String> alphabet = new LinkedHashSet<>(afnd.alphabet());
        Set<String> accepting = new LinkedHashSet<>(afnd.acceptingStates());

        // Estado inicial del AFD es el conjunto con el estado inicial del AFND.
        Set<String> initialSet = new TreeSet<>(List.of(afnd.initialState()));
        String initialLabel = formatStateSet(initialSet);

        // Mapeo etiqueta -> conjunto real para poder expandir transiciones.
        Map<String, Set<String>> labelToSet = new LinkedHashMap<>();
        labelToSet.put(initialLabel, initialSet);

        // BFS sobre superestados para generar todos los estados alcanzables.
        Deque<String> queue = new ArrayDeque<>();
        queue.add(initialLabel);

        Map<String, Map<String, String>> transitions = new LinkedHashMap<>();
        List<String> states = new ArrayList<>();
        boolean needsTrap = false;

        while (!queue.isEmpty()) {
            String currentLabel = queue.removeFirst();
            Set<String> currentSet = labelToSet.get(currentLabel);
            states.add(currentLabel);

            Map<String, String> stateTransitions = new LinkedHashMap<>();
            for (String symbol : alphabet) {
                // Calcular destino como union de transiciones del conjunto actual.
                Set<String> destinationSet = new TreeSet<>();
                for (String state : currentSet) {
                    destinationSet.addAll(afnd.transitions()
                            .getOrDefault(state, Map.of())
                            .getOrDefault(symbol, List.of()));
                }

                if (destinationSet.isEmpty()) {
                    // Si no hay destinos, se va al estado trampa.
                    stateTransitions.put(symbol, TRAP_STATE);
                    needsTrap = true;
                    continue;
                }

                String destinationLabel = formatStateSet(destinationSet);
                stateTransitions.put(symbol, destinationLabel);

                // Registrar nuevos superestados a explorar.
                if (!labelToSet.containsKey(destinationLabel)) {
                    labelToSet.put(destinationLabel, destinationSet);
                    queue.addLast(destinationLabel);
                }
            }
            transitions.put(currentLabel, stateTransitions);
        }

        // Completar transiciones del estado trampa si fue necesario.
        if (needsTrap) {
            if (!states.contains(TRAP_STATE)) {
                states.add(TRAP_STATE);
            }
            Map<String, String> trapTransitions = new LinkedHashMap<>();
            for (String symbol : alphabet) {
                trapTransitions.put(symbol, TRAP_STATE);
            }
            transitions.put(TRAP_STATE, trapTransitions);
        }

        // Un superestado es de aceptacion si intersecta con aceptaciones del AFND.
        List<String> acceptingStates = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : labelToSet.entrySet()) {
            if (!disjoint(entry.getValue(), accepting)) {
                acceptingStates.add(entry.getKey());
            }
        }

        return new DeterministicAutomaton(
                List.copyOf(states),
                List.copyOf(alphabet),
                initialLabel,
                List.copyOf(acceptingStates),
                transitions
        );
    }

    /**
     * Minimiza un AFD por el metodo de tabla de distincion.
     * Considera solo estados alcanzables para evitar clases inutiles.
     */
    public static DeterministicAutomaton minimizeAfd(DeterministicAutomaton afd) {
        List<String> alphabet = afd.alphabet();
        Set<String> accepting = new LinkedHashSet<>(afd.acceptingStates());
        Set<String> reachable = reachableStates(afd);

        // Considerar solo estados alcanzables para la tabla.
        List<String> states = reachable.stream().sorted().toList();
        Map<StatePair, Boolean> table = new LinkedHashMap<>();

        // Inicializar pares distinguibles por aceptacion.
        for (int i = 0; i < states.size(); i++) {
            for (int j = i + 1; j < states.size(); j++) {
                String a = states.get(i);
                String b = states.get(j);
                boolean distinguishable = accepting.contains(a) != accepting.contains(b);
                table.put(StatePair.of(a, b), distinguishable);
            }
        }

        // Propagar distinciones mientras haya cambios.
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Map.Entry<StatePair, Boolean> entry : new ArrayList<>(table.entrySet())) {
                if (entry.getValue()) {
                    continue;
                }
                String a = entry.getKey().a();
                String b = entry.getKey().b();
                for (String symbol : alphabet) {
                    String destinationA = transition(afd.transitions(), a, symbol);
                    String destinationB = transition(afd.transitions(), b, symbol);

                    if (destinationA == null || destinationB == null) {
                        continue;
                    }
                    if (destinationA.equals(destinationB)) {
                        continue;
                    }

                    StatePair destinations = StatePair.of(destinationA, destinationB);
                    if (table.getOrDefault(destinations, false)) {
                        table.put(entry.getKey(), true);
                        changed = true;
                        break;
                    }
                }
            }
        }

        // Union-find simple usando un mapa de representantes.
        Map<String, String> representative = new LinkedHashMap<>();
        for (String state : states) {
            representative.put(state, state);
        }

        // Unir pares no distinguibles.
        for (Map.Entry<StatePair, Boolean> entry : table.entrySet()) {
            if (!entry.getValue()) {
                String ra = find(representative, entry.getKey().a());
                String rb = find(representative, entry.getKey().b());
                if (!ra.equals(rb)) {
                    representative.put(rb, ra);
                }
            }
        }

        // Construir grupos de equivalencia.
        Map<String, Set<String>> groups = new LinkedHashMap<>();
        for (String state : states) {
            String root = find(representative, state);
            groups.computeIfAbsent(root, key -> new LinkedHashSet<>()).add(state);
        }

        // Elegir representante canonico por grupo.
        Map<String, String> repMap = new LinkedHashMap<>();
        List<String> minimizedStates = new ArrayList<>();
        for (Set<String> group : groups.values()) {
            String rep = group.stream().sorted().findFirst().orElseThrow();
            minimizedStates.add(rep);
            for (String state : group) {
                repMap.put(state, rep);
            }
        }

        // Generar transiciones del AFD minimizado.
        Map<String, Map<String, String>> minimizedTransitions = new LinkedHashMap<>();
        for (String rep : minimizedStates) {
            Map<String, String> stateTransitions = new LinkedHashMap<>();
            for (String symbol : alphabet) {
                String destination = transition(afd.transitions(), rep, symbol);
                if (destination == null) {
                    continue;
                }
                stateTransitions.put(symbol, repMap.get(destination));
            }
            minimizedTransitions.put(rep, stateTransitions);
        }

        // Proyectar estados de aceptacion sobre los representantes.
        Set<String> minimizedAccepting = new LinkedHashSet<>();
        for (String state : accepting) {
            if (reachable.contains(state)) {
                minimizedAccepting.add(repMap.get(state));
            }
        }

        String minimizedInitial = repMap.get(afd.initialState());

        return new DeterministicAutomaton(
                minimizedStates.stream().sorted().toList(),
                alphabet,
                minimizedInitial,
                minimizedAccepting.stream().sorted().toList(),
                minimizedTransitions
        );
    }

    /**
     * Convierte un AFD a la definicion usada por la UI.
     * Adaptando las transiciones deterministas a listas unitarias.
     */
    public static AutomatonDefinition toDefinition(
            DeterministicAutomaton afd,
            int exerciseId,
            String name,
            AutomataType automataType
    ) {
        return new AutomatonDefinition(
                exerciseId,
                name,
                automataType,
                afd.states(),
                afd.alphabet(),
                afd.initialState(),
                afd.acceptingStates(),
                toListTransitionMap(afd.transitions())
        );
    }

    /**
     * Simula un AFND manteniendo el conjunto actual de estados.
     * Cada simbolo actualiza el conjunto con la union de destinos.
     */
    public static SimulationResult simulateAfnd(AutomatonDefinition afnd, String input) {
        Set<String> currentStates = new TreeSet<>(Set.of(afnd.initialState()));
        List<SimulationStep> steps = new ArrayList<>();

        for (char symbol : input.toCharArray()) {
            String symbolString = String.valueOf(symbol);
            Set<String> nextStates = new TreeSet<>();
            for (String currentState : currentStates) {
                nextStates.addAll(afnd.transitions()
                        .getOrDefault(currentState, Map.of())
                        .getOrDefault(symbolString, List.of()));
            }
            steps.add(new SimulationStep(symbolString, formatStateSet(currentStates), formatStateSet(nextStates)));
            currentStates = nextStates;
        }

        boolean accepted = !disjoint(currentStates, new LinkedHashSet<>(afnd.acceptingStates()));
        return new SimulationResult(accepted, steps, formatStateSet(currentStates), accepted);
    }

    /**
     * Simula un AFD recorriendo transiciones deterministas.
     * Si falta transicion explicita, se conserva el estado actual.
     */
    public static SimulationResult simulateAfd(DeterministicAutomaton afd, String input) {
        String currentState = afd.initialState();
        List<SimulationStep> steps = new ArrayList<>();

        for (char symbol : input.toCharArray()) {
            String symbolString = String.valueOf(symbol);
            String nextState = transition(afd.transitions(), currentState, symbolString);
            if (nextState == null) {
                nextState = currentState;
            }
            steps.add(new SimulationStep(symbolString, currentState, nextState));
            currentState = nextState;
        }

        boolean accepted = afd.acceptingStates().contains(currentState);
        return new SimulationResult(accepted, steps, currentState, accepted);
    }

    // Adaptador de transiciones AFD a formato AFND esperado por la UI.
    private static Map<String, Map<String, List<String>>> toListTransitionMap(Map<String, Map<String, String>> transitions) {
        Map<String, Map<String, List<String>>> converted = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, String>> stateEntry : transitions.entrySet()) {
            Map<String, List<String>> stateTransitions = new LinkedHashMap<>();
            for (Map.Entry<String, String> transitionEntry : stateEntry.getValue().entrySet()) {
                stateTransitions.put(transitionEntry.getKey(), List.of(transitionEntry.getValue()));
            }
            converted.put(stateEntry.getKey(), stateTransitions);
        }
        return converted;
    }

    // Obtiene estados alcanzables desde el inicial.
    private static Set<String> reachableStates(DeterministicAutomaton afd) {
        Set<String> visited = new LinkedHashSet<>();
        Deque<String> stack = new ArrayDeque<>();
        stack.push(afd.initialState());

        while (!stack.isEmpty()) {
            String current = stack.pop();
            if (!visited.add(current)) {
                continue;
            }
            for (String symbol : afd.alphabet()) {
                String destination = transition(afd.transitions(), current, symbol);
                if (destination != null) {
                    stack.push(destination);
                }
            }
        }

        return visited;
    }

    // Devuelve el destino para una transicion determinista.
    private static String transition(Map<String, Map<String, String>> transitions, String state, String symbol) {
        String destination = transitions.getOrDefault(state, Map.of()).get(symbol);
        if (destination == null && transitions.containsKey(TRAP_STATE)) {
            return TRAP_STATE;
        }
        return destination;
    }

    // Formatea un conjunto como etiqueta de superestado.
    private static String formatStateSet(Collection<String> states) {
        if (states.isEmpty()) {
            return TRAP_STATE;
        }
        return "{" + String.join(",", new TreeSet<>(states)) + "}";
    }

    // Devuelve true si dos conjuntos no comparten elementos.
    private static boolean disjoint(Set<String> left, Set<String> right) {
        for (String value : left) {
            if (right.contains(value)) {
                return false;
            }
        }
        return true;
    }

    // Busca el representante en la estructura union-find simple.
    private static String find(Map<String, String> representative, String state) {
        String current = state;
        while (!representative.get(current).equals(current)) {
            current = representative.get(current);
        }
        return current;
    }

    // Par canonico para indexar la tabla de distincion.
    private record StatePair(String a, String b) {
        private static StatePair of(String first, String second) {
            if (first.compareTo(second) <= 0) {
                return new StatePair(first, second);
            }
            return new StatePair(second, first);
        }
    }
}
