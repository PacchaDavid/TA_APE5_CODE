package com.afd_converter.ta_ape5.automaton.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import com.afd_converter.ta_ape5.automaton.api.AutomatonType;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad JPA que representa un autómata finito persistido en la base de datos.
 * Almacena el nombre, tipo (AFD/AFND), alfabeto, estados y transiciones.
 */
@Entity
@Table(name = "automatons")
public class Automaton {
	// Identificador único del autómata
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// Nombre descriptivo del autómata
	@Column(nullable = false, length = 255)
	private String name;

	// Tipo del autómata: AFD (Finito Determinista) o AFND (Finito No Determinista)
	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private AutomatonType type;

	// Alfabeto del autómata (símbolos permitidos), almacenado como texto
	@Column(nullable = false, columnDefinition = "TEXT")
	private String alphabet;

	// Estados del autómata (q0, q1, q2, etc.), almacenados como texto
	@Column(nullable = false, columnDefinition = "TEXT")
	private String states;

	// Estado inicial del autómata (donde comienza el reconocimiento)
	@Column(nullable = false, length = 100)
	private String initialState;

	// Estados de aceptación del autómata (estados finales), almacenados como texto
	@Column(nullable = false, columnDefinition = "TEXT")
	private String acceptingStates;

	// Relación uno-a-muchos: un autómata tiene múltiples transiciones
	@OneToMany(mappedBy = "automaton", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Transition> transitions = new ArrayList<>();

	// Marca de tiempo de creación del registro
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	// Marca de tiempo de la última actualización del registro
	@Column(nullable = false)
	private LocalDateTime updatedAt;

	// Constructores
	public Automaton() {
		this.createdAt = LocalDateTime.now();
		this.updatedAt = LocalDateTime.now();
	}

	public Automaton(String name, AutomatonType type, String alphabet, String states, String initialState, String acceptingStates) {
		this();
		this.name = name;
		this.type = type;
		this.alphabet = alphabet;
		this.states = states;
		this.initialState = initialState;
		this.acceptingStates = acceptingStates;
	}

	// Getters y Setters
	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }

	public String getName() { return name; }
	public void setName(String name) { this.name = name; }

	public AutomatonType getType() { return type; }
	public void setType(AutomatonType type) { this.type = type; }

	public String getAlphabet() { return alphabet; }
	public void setAlphabet(String alphabet) { this.alphabet = alphabet; }

	public String getStates() { return states; }
	public void setStates(String states) { this.states = states; }

	public String getInitialState() { return initialState; }
	public void setInitialState(String initialState) { this.initialState = initialState; }

	public String getAcceptingStates() { return acceptingStates; }
	public void setAcceptingStates(String acceptingStates) { this.acceptingStates = acceptingStates; }

	public List<Transition> getTransitions() { return transitions; }
	public void setTransitions(List<Transition> transitions) { this.transitions = transitions; }

	public LocalDateTime getCreatedAt() { return createdAt; }
	public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

	public LocalDateTime getUpdatedAt() { return updatedAt; }
	public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
