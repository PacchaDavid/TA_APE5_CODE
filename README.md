# TA_APE5_CODE

Aplicación web para trabajar con autómatas finitos en el contexto de los ejercicios 4, 5 y 6. El sistema permite cargar la definición formal del autómata, simular cadenas de entrada, visualizar el recorrido de estados y comparar los resultados contra un conjunto de pruebas predefinidas.

## Descripción del sistema

El proyecto está dividido en dos partes:

- Backend en Java con Spring Boot, encargado de exponer una API REST con las definiciones, simulaciones y pruebas de cada ejercicio.
- Frontend estático en HTML, CSS y JavaScript, que consume la API y presenta el autómata como tabla, diagrama y resultados de simulación.

La interfaz permite cambiar entre tres tipos de autómata para cada ejercicio:

- AFND original
- AFD equivalente
- AFD minimizado

Los ejercicios incluidos son:

- Ejercicio 4: detección de ataque SYN flood
- Ejercicio 5: validación de protocolo IoT
- Ejercicio 6: reconocimiento de secuencias genéticas

## Estructura del proyecto

- `backend/`: API REST con Spring Boot.
- `frontend/`: aplicación web estática y servidor local para desarrollo.

## Requisitos

- Java 17 o superior.
- Maven Wrapper incluido en el proyecto.
- Python 3.8 o superior para servir el frontend localmente.

## Cómo ejecutar el sistema

### 1. Iniciar el backend

Desde la carpeta raíz del proyecto:

```bash
cd backend
./mvnw spring-boot:run
```

El backend se levanta en `http://localhost:8080`.

### 2. Iniciar el frontend

En otra terminal, desde la carpeta raíz del proyecto:

```bash
python3 frontend/server.py --port 8000
```

Luego abre `http://localhost:8000` en el navegador.

### 3. Uso de la aplicación

- Selecciona el ejercicio que quieres explorar.
- Cambia el tipo de autómata para comparar AFND, AFD y AFD minimizado.
- Escribe una cadena y ejecuta la simulación.
- Revisa el recorrido paso a paso en la tarjeta de resultados y el diagrama interactivo.
- Usa las cadenas de ejemplo para probar casos rápidos.

## API REST

La interfaz consume la API en `http://localhost:8080/api/automata`.

Endpoints principales:

- `POST /api/automata/simulate`: simula una cadena para un ejercicio y tipo de autómata.
- `GET /api/automata/definition/{exerciseId}`: obtiene la definición formal del autómata.
- `GET /api/automata/test/{exerciseId}`: devuelve los casos de prueba del ejercicio.

### Ejemplo de simulación

```bash
curl -X POST http://localhost:8080/api/automata/simulate \
	-H "Content-Type: application/json" \
	-d '{"exerciseId":4,"automataType":"AFND","input":"sar"}'
```

## Pruebas

Para ejecutar las pruebas del backend:

```bash
cd backend
./mvnw test
```

## Notas de desarrollo

- El frontend apunta al backend en `frontend/js/api.js`.
- Si cambias el puerto del backend, actualiza la URL base de la API en ese archivo.
- El backend permite CORS para facilitar el uso desde el frontend local.
