# Contexto del Backend - TA_APE5

## 1. Objetivo del sistema
Este backend en Spring Boot permite:
- Convertir un AFND a AFD usando el algoritmo de construccion de subconjuntos.
- Minimizar un AFD usando el algoritmo de tabla de equivalencia de estados.
- Devolver siempre resultados en JSON, incluyendo el paso a paso de cada algoritmo para que el frontend lo pinte.

## 2. Stack y arquitectura
- Lenguaje: Java 17
- Framework: Spring Boot
- Patron: MVC
- Persistencia: Spring Data JPA
- Base de datos: PostgreSQL
- Infraestructura local: Docker Compose

Estructura principal:
- Capa API/DTOs: contratos JSON de entrada y salida.
- Capa Controller: endpoints REST.
- Capa Service: logica de conversion y minimizacion.
- Capa Domain/Repository: entidades JPA y repositorios.
- Capa Exception: manejo centralizado de errores.

## 3. Historias de usuario implementadas
### HU 1: Convertir AFND a AFD
Implementado en servicio de conversion con:
- Validaciones de estructura de AFND.
- Cerradura epsilon (epsilon-closure).
- Funcion move por simbolo.
- Expansion de subconjuntos.
- Resultado final AFD y pasos detallados del algoritmo.

### HU 2: Minimizar AFD
Implementado en servicio de minimizacion con:
- Validacion de que la entrada sea AFD determinista.
- Eliminacion de estados no alcanzables.
- Completado con estado sumidero si faltan transiciones.
- Tabla de equivalencia:
  - Marcado inicial final/no final.
  - Marcado iterativo por dependencia de transiciones.
- Construccion de clases de equivalencia.
- Construccion de AFD minimizado.
- Salida con snapshots de tabla para frontend.

## 4. Endpoints actuales
Base path: /api/automata

- POST /convert
  - Entrada: AFND en JSON.
  - Salida: AFD en JSON + steps.

- POST /minimize
  - Entrada: AFD en JSON.
  - Salida: AFD minimizado + steps + tableSteps.

## 5. Contratos JSON clave
Campos de entrada esperados:
- type: AFD o AFND
- name
- alphabet
- states
- initialState
- acceptingStates
- transitions (from, symbol, to)

Campos de salida para minimizacion:
- automaton
- steps
- tableSteps (iteraciones de la tabla con celdas marcadas/no marcadas)

## 6. Persistencia y base de datos
- Entidades:
  - Automaton
  - Transition
- Repositorios:
  - AutomatonRepository
  - TransitionRepository

Configuracion de DB en application.properties:
- datasource PostgreSQL local
- Hibernate/JPA habilitado
- Estrategia DDL actualmente orientada a pruebas (create-drop cuando se configure asi)

## 7. Docker Compose
Archivo en raiz del repo para levantar PostgreSQL localmente.
Servicio configurado con:
- Imagen postgres:16-alpine
- Puerto 5432
- Volumen persistente
- Healthcheck

## 8. Calidad y pruebas
Pruebas unitarias actuales cubren:
- Conversion AFND -> AFD.
- Minimización de AFD y validacion de salida.
- Validacion de error cuando se intenta minimizar un tipo no AFD.

Build verificado con Maven test en estado exitoso.

## 9. Decisiones de diseño importantes
- Se priorizo devolver trazabilidad del algoritmo (steps y tableSteps) para facilitar visualizacion en frontend.
- La minimizacion usa una representacion de tabla apta para pintar celdas y motivos de marcado.
- Se separo conversion y minimizacion en servicios distintos para mantener cohesion y facilitar mantenimiento.
