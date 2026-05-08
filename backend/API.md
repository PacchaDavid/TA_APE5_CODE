# API Backend (TA_APE5)

Base URL (local): `http://localhost:8080`

Content-Type: `application/json`

## Esquemas

### AutomatonRequest

Campos:

1. `type` (string, requerido): `AFD` o `AFND`.
1. `name` (string, requerido, no vacío)
1. `alphabet` (string[], requerido, min 1, elementos no vacíos)
1. `states` (string[], requerido, min 1, elementos no vacíos)
1. `initialState` (string, requerido, no vacío)
1. `acceptingStates` (string[], requerido, min 1, elementos no vacíos)
1. `transitions` (TransitionRequest[], requerido, min 1)

### TransitionRequest

1. `from` (string, requerido, no vacío)
1. `symbol` (string, requerido, no vacío)
1. `to` (string[], requerido, min 1, elementos no vacíos)

### ApiErrorResponse (errores 400)

1. `timestamp` (string, RFC3339)
1. `status` (number)
1. `error` (string)
1. `message` (string)

## Endpoint: Convertir AFND a AFD

`POST /api/automata/convert`

Convierte un **AFND** a un **AFD** equivalente (construcción de subconjuntos). Devuelve el AFD y el paso a paso.

### Reglas de negocio

1. `type` debe ser `AFND`.
1. `initialState` debe existir en `states`.
1. `acceptingStates` deben existir en `states`.
1. `symbol`:
1. Se acepta epsilon: `epsilon`, `eps` o `ε` (se normaliza a `epsilon`).
1. Si no es epsilon, debe pertenecer a `alphabet`.
1. Cada destino en `to` debe existir en `states`.

### Response 200 (ConversionResponse)

1. `automaton` (AutomatonResponse)
1. `steps` (ConversionStep[])

Notas del resultado:

1. Los estados del AFD son nombres canónicos de subconjuntos, por ejemplo: `{q0,q1}`.
1. Si se alcanza el conjunto vacío, se usa el estado sumidero `EMPTY_SET`.

### Ejemplo `curl`

```bash
curl -sS -X POST "http://localhost:8080/api/automata/convert" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "AFND",
    "name": "EjemploAFND",
    "alphabet": ["a","b"],
    "states": ["q0","q1","q2"],
    "initialState": "q0",
    "acceptingStates": ["q2"],
    "transitions": [
      {"from":"q0","symbol":"epsilon","to":["q1"]},
      {"from":"q1","symbol":"a","to":["q1","q2"]},
      {"from":"q1","symbol":"b","to":["q1"]},
      {"from":"q2","symbol":"b","to":["q2"]}
    ]
  }'
```

## Endpoint: Minimizar AFD

`POST /api/automata/minimize`

Minimiza un **AFD** usando el algoritmo de tabla de equivalencia de estados. Devuelve el AFD minimizado, el paso a paso y snapshots de la tabla.

### Reglas de negocio

1. `type` debe ser `AFD`.
1. No se aceptan transiciones epsilon.
1. Cada transición debe tener exactamente **un** destino (`to` con 1 elemento).
1. `symbol` debe pertenecer a `alphabet`.

Notas del resultado:

1. Se descartan estados no alcanzables desde `initialState`.
1. Si el AFD no está completo, el servicio agrega un estado sumidero `DEAD_STATE` para completar transiciones faltantes antes de minimizar.
1. `automaton.name` se devuelve con sufijo `-MIN`.

### Response 200 (MinimizationResponse)

1. `automaton` (AutomatonResponse)
1. `steps` (ConversionStep[])
1. `tableSteps` (MinimizationTableStep[])

### Ejemplo `curl`

```bash
curl -sS -X POST "http://localhost:8080/api/automata/minimize" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "AFD",
    "name": "EjemploAFD",
    "alphabet": ["0","1"],
    "states": ["A","B","C","D"],
    "initialState": "A",
    "acceptingStates": ["C","D"],
    "transitions": [
      {"from":"A","symbol":"0","to":["B"]},
      {"from":"A","symbol":"1","to":["C"]},
      {"from":"B","symbol":"0","to":["A"]},
      {"from":"B","symbol":"1","to":["D"]},
      {"from":"C","symbol":"0","to":["C"]},
      {"from":"C","symbol":"1","to":["C"]},
      {"from":"D","symbol":"0","to":["C"]},
      {"from":"D","symbol":"1","to":["C"]}
    ]
  }'
```

## OpenAPI

Contrato OpenAPI: `backend/openapi.yaml`.

## Endpoint: Convertir y minimizar (1 paso)

`POST /api/automata/convert-minimize`

Convierte un **AFND** a **AFD** y luego minimiza el AFD en una sola operación.

### Request

Body: `AutomatonRequest` con `type = AFND` (mismas reglas que `POST /api/automata/convert`).

### Response 200 (ConvertMinimizeResponse)

1. `automaton` (AutomatonResponse): el **AFD minimizado**.
1. `conversionSteps` (ConversionStep[]): pasos del algoritmo AFND -> AFD.
1. `minimizationSteps` (ConversionStep[]): pasos del algoritmo de minimización.
1. `minimizationTableSteps` (MinimizationTableStep[]): tabla (snapshots) de equivalencia.

### Ejemplo `curl`

```bash
curl -sS -X POST "http://localhost:8080/api/automata/convert-minimize" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "AFND",
    "name": "EjemploAFND",
    "alphabet": ["a","b"],
    "states": ["q0","q1","q2"],
    "initialState": "q0",
    "acceptingStates": ["q2"],
    "transitions": [
      {"from":"q0","symbol":"epsilon","to":["q1"]},
      {"from":"q1","symbol":"a","to":["q1","q2"]},
      {"from":"q1","symbol":"b","to":["q1"]},
      {"from":"q2","symbol":"b","to":["q2"]}
    ]
  }'
```
