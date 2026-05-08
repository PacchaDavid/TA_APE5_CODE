package com.afd_converter.ta_ape5.automaton.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Entidad JPA que representa una transición de un autómata.
 * Una transición define cómo pasar de un estado a otro con un símbolo.
 */
@Entity
@Table(name = "transitions")
public class Transition {
	// Identificador único de la transición
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// Referencia al autómata al que pertenece esta transición
	@ManyToOne(optional = false)
	@JoinColumn(name = "automaton_id", nullable = false)
	private Automaton automaton;

	// Estado origen de la transición (desde dónde salimos)
	@Column(nullable = false, length = 100)
	private String fromState;

	// Símbolo del alfabeto que provoca la transición
	@Column(nullable = false, length = 100)
	private String symbol;

	// Estado o estados destino de la transición (a dónde vamos)
	// Para AFD: un único estado
	// Para AFND: múltiples estados posibles (almacenados como texto delimitado)
	@Column(nullable = false, columnDefinition = "TEXT")
	private String toStates;

	// Constructores
	public Transition() {}

	public Transition(Automaton automaton, String fromState, String symbol, String toStates) {
		this.automaton = automaton;
		this.fromState = fromState;
		this.symbol = symbol;
		this.toStates = toStates;
	}

	// Getters y Setters
	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }

	public Automaton getAutomaton() { return automaton; }
	public void setAutomaton(Automaton automaton) { this.automaton = automaton; }

	public String getFromState() { return fromState; }
	public void setFromState(String fromState) { this.fromState = fromState; }

	public String getSymbol() { return symbol; }
	public void setSymbol(String symbol) { this.symbol = symbol; }

	public String getToStates() { return toStates; }
	public void setToStates(String toStates) { this.toStates = toStates; }
}
