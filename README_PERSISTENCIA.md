# SISTEMA DE CONVERSIÓN AFND → AFD CON MINIMIZACIÓN

## Descripción

Sistema backend en Spring Boot para convertir autómatas finitos no deterministas (AFND) en autómatas finitos deterministas (AFD) usando el algoritmo de construcción de subconjuntos. También permite minimizar el AFD resultante.

## Requisitos previos

- Docker y Docker Compose instalados
- Java 17 o superior
- Maven 3.8 o superior

## Instalación y Ejecución

### 1. Inicia la base de datos PostgreSQL con Docker Compose

Desde la raíz del proyecto:

```bash
docker-compose up -d
```

Este comando:
- Levanta un contenedor PostgreSQL
- Crea la base de datos `ta_ape5_db`
- Expone PostgreSQL en `localhost:5432`

Verifica que el contenedor esté corriendo:

```bash
docker-compose ps
```

Para ver los logs:

```bash
docker-compose logs -f postgres
```

### 2. Compila el backend

```bash
cd backend
./mvnw clean install
```

### 3. Ejecuta la aplicación

```bash
./mvnw spring-boot:run
```

La aplicación estará disponible en `http://localhost:8080`

## API Endpoints

### Convertir AFND a AFD

**Endpoint:** `POST /api/automata/convert`

**Ejemplo de solicitud:**

```json
{
  "type": "AFND",
  "name": "Ejemplo AFND",
  "alphabet": ["0", "1"],
  "states": ["q0", "q1", "q2"],
  "initialState": "q0",
  "acceptingStates": ["q2"],
  "transitions": [
    {
      "from": "q0",
      "symbol": "0",
      "to": ["q0", "q1"]
    },
    {
      "from": "q0",
      "symbol": "1",
      "to": ["q0"]
    },
    {
      "from": "q1",
      "symbol": "1",
      "to": ["q2"]
    },
    {
      "from": "q2",
      "symbol": "0",
      "to": ["q2"]
    },
    {
      "from": "q2",
      "symbol": "1",
      "to": ["q2"]
    }
  ]
}
```

**Respuesta exitosa (HTTP 200):**

```json
{
  "automaton": {
    "name": "Ejemplo AFND-AFD",
    "type": "AFD",
    "alphabet": ["0", "1"],
    "states": ["{q0}", "{q0,q1}", "{q0,q2}", "{q0,q1,q2}"],
    "initialState": "{q0}",
    "acceptingStates": ["{q0,q1,q2}", "{q0,q2}"],
    "transitions": [...]
  },
  "steps": [
    {
      "order": 1,
      "title": "Validación",
      "detail": "Se recibió un AFND llamado 'Ejemplo AFND' con 3 estados y alfabeto [0, 1]."
    },
    {
      "order": 2,
      "title": "Estado inicial",
      "detail": "La cerradura epsilon del estado inicial 'q0' es {q0}."
    },
    ...
  ]
}
```

## Estructura del Proyecto

```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/afd_converter/ta_ape5/
│   │   │   ├── automaton/
│   │   │   │   ├── api/              # DTOs y contratos HTTP
│   │   │   │   ├── controller/       # Controladores REST
│   │   │   │   ├── domain/           # Entidades JPA para persistencia
│   │   │   │   ├── exception/        # Manejo de excepciones
│   │   │   │   ├── repository/       # Repositorios JPA
│   │   │   │   └── service/          # Lógica de negocio (algoritmo de conversión)
│   │   │   └── TaApe5Application.java  # Punto de entrada
│   │   └── resources/
│   │       └── application.properties   # Configuración de conexión DB
│   └── test/                            # Pruebas unitarias
├── pom.xml                              # Dependencias Maven
└── docker-compose.yml                   # Configuración de infraestructura

```

## Mantenimiento de la Base de Datos

### Ver contenedores activos

```bash
docker-compose ps
```

### Detener la base de datos

```bash
docker-compose down
```

### Reiniciar la base de datos

```bash
docker-compose restart postgres
```

### Acceder a la base de datos directamente

```bash
docker exec -it ta_ape5_postgres psql -U ta_ape5_user -d ta_ape5_db
```

Dentro de psql:

```sql
-- Ver tablas
\dt

-- Ver estructura de tabla
\d automatons

-- Consulta básica
SELECT * FROM automatons;
```

### Limpiar datos y volúmenes

```bash
# Detiene contenedores y elimina volúmenes
docker-compose down -v
```

## Notas de Desarrollo

- El archivo `application.properties` contiene la configuración de PostgreSQL
- Hibernate genera automáticamente las tablas al iniciar (ddl-auto=update)
- Los comentarios en español facilitan la comprensión del código
- El algoritmo de construcción de subconjuntos está documentado en el servicio

## Algoritmo de Conversión

El sistema utiliza el **Algoritmo de Construcción de Subconjuntos**:

1. Comienza con la cerradura epsilon del estado inicial
2. Para cada estado nuevo descubierto:
   - Para cada símbolo del alfabeto:
     - Calcula el movimiento (move) desde ese estado
     - Aplica cerradura epsilon al resultado
     - Si es nuevo, lo agrega a procesamiento
3. Un estado del AFD es de aceptación si contiene al menos un estado de aceptación del AFND

## Próximas Historias de Usuario

- [ ] Minimización del AFD generado
- [ ] Endpoints REST para persistencia de autómatas
- [ ] UI web para visualización de autómatas
- [ ] Importar/exportar autómatas en diferentes formatos
