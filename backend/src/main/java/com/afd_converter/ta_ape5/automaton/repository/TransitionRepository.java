package com.afd_converter.ta_ape5.automaton.repository;

import com.afd_converter.ta_ape5.automaton.domain.Transition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio JPA para la entidad Transition.
 * Proporciona métodos CRUD y consultas para acceder a las transiciones en la base de datos.
 */
@Repository
public interface TransitionRepository extends JpaRepository<Transition, Long> {
	// Busca todas las transiciones de un autómata específico
	List<Transition> findByAutomatonId(Long automatonId);

	// Busca transiciones que salen desde un estado específico en un autómata
	List<Transition> findByAutomatonIdAndFromState(Long automatonId, String fromState);

	// Busca una transición específica por autómata, estado origen y símbolo
	List<Transition> findByAutomatonIdAndFromStateAndSymbol(Long automatonId, String fromState, String symbol);
}
