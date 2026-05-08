package com.afd_converter.ta_ape5.automaton.repository;

import com.afd_converter.ta_ape5.automaton.domain.Automaton;
import com.afd_converter.ta_ape5.automaton.api.AutomatonType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para la entidad Automaton.
 * Proporciona métodos CRUD y consultas personalizadas a la base de datos.
 */
@Repository
public interface AutomatonRepository extends JpaRepository<Automaton, Long> {
	// Busca un autómata por su nombre exacto
	Optional<Automaton> findByName(String name);

	// Busca todos los autómatas de un tipo específico (AFD o AFND)
	List<Automaton> findByType(AutomatonType type);

	// Busca autómatas cuyo nombre contenga una cadena (búsqueda parcial)
	List<Automaton> findByNameContainingIgnoreCase(String namePart);
}
