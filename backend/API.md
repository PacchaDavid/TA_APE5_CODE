# API Backend (TA_APE5)

Base URL (local): `http://localhost:8080`

Content-Type: `application/json`

## Endpoint: Convertir AFND a AFD

`POST /api/automata/convert`

Convierte un **AFND** (Autómata Finito No Determinista) a un **AFD** equivalente usando construcción de subconjuntos. Devuelve el AFD resultante y el paso a paso del algoritmo.

### Request (AutomatonRequest)

Campos:

1. `type` (string, requerido): debe ser `AFND`.
1. `name` (string, requerido, no vacío)
1. `alphabet` (string[], requerido, min 1, elementos no vacíos): símbolos del alfabeto.
1. `states` (string[], requerido, min 1, elementos no vacíos)
1. `initialState` (string, requerido, no vacío): debe existir en `states`.
1. `acceptingStates` (string[], requerido, min 1, elementos no vacíos): deben existir en `states`.
1. `transitions` (TransitionRequest[], requerido, min 1)

#### TransitionRequest

1. `from` (string, requerido, no vacío): debe existir en `states`.
1. `symbol` (string, requerido, no vacío):
1. Si es epsilon: se acepta `epsilon`, `eps` o `ε` (se normaliza internamente a `epsilon`).
1. Si no es epsilon: debe pertenecer a `alphabet`.
1. `to` (string[], requerido, min 1, elementos no vacíos): cada destino debe existir en `states`.

### Response 200 (ConversionResponse)

1. `automaton` (AutomatonResponse): el AFD generado.
1. `steps` (ConversionStep[]): lista ordenada de pasos para visualización.

#### AutomatonResponse

1. `name` (string)
1. `type` (string): `AFD`
1. `alphabet` (string[])
1. `states` (string[])
1. `initialState` (string)
1. `acceptingStates` (string[])
1. `transitions` (TransitionResponse[])

#### TransitionResponse

1. `from` (string)
1. `symbol` (string)
1. `to` (string[]): en el AFD se devuelve como lista (típicamente de 1 elemento).

#### ConversionStep

1. `order` (number)
1. `title` (string)
1. `detail` (string)

Notas del resultado:

1. Los estados del AFD son nombres canónicos de subconjuntos, por ejemplo: `{q0,q1}`.
1. Si se alcanza el conjunto vacío, se usa el estado sumidero `EMPTY_SET`.

### Errores 400 (ApiErrorResponse)

El backend responde con status `400` en:

1. Errores de validación Jakarta (`@NotBlank`, `@NotEmpty`, etc.).
1. Errores de dominio (por ejemplo: `type != AFND`, símbolos fuera del alfabeto, estados inexistentes).

Formato:

1. `timestamp` (string, RFC3339)
1. `status` (number)
1. `error` (string)
1. `message` (string)

## Ejemplos

### Convertir un AFND (con epsilon)

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

Respuesta (ejemplo recortado):

```json
{
  "automaton": {
    "name": "EjemploAFND-AFD",
    "type": "AFD",
    "alphabet": ["a","b"],
    "states": ["{q0,q1}","{q1}","{q1,q2}","{q2}","EMPTY_SET"],
    "initialState": "{q0,q1}",
    "acceptingStates": ["{q1,q2}","{q2}"],
    "transitions": [
      {"from":"{q0,q1}","symbol":"a","to":["{q1,q2}"]},
      {"from":"{q0,q1}","symbol":"b","to":["{q1}"]}
    ]
  },
  "steps": [
    {"order":1,"title":"Validación","detail":"..."},
    {"order":2,"title":"Estado inicial","detail":"..."}
  ]
}
```

### Error: tipo inválido

```bash
curl -sS -X POST "http://localhost:8080/api/automata/convert" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "AFD",
    "name": "NoValido",
    "alphabet": ["a"],
    "states": ["q0"],
    "initialState": "q0",
    "acceptingStates": ["q0"],
    "transitions": [{"from":"q0","symbol":"a","to":["q0"]}]
  }'
```

```json
{
  "timestamp": "2026-05-08T10:15:30.123Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Esta operación solo acepta un AFND como entrada."
}
```

## OpenAPI

El contrato OpenAPI está en `backend/openapi.yaml`.
