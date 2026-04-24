# Smart Campus Sensor & Room Management API

A RESTful API for managing campus rooms and IoT sensors, built with **JAX-RS (Jersey 2.39.1)** and an embedded **Grizzly HTTP server**. No external server required — runs as a standalone JAR.

---

## How to Run

### Prerequisites

- Java JDK 11 or higher
- Maven 3.6+ (NetBeans ships with Maven built in)
- Internet connection (first run only — Maven downloads dependencies automatically)

### Run in NetBeans

1. File → Open Project → select the `smart-campus` folder
2. Right-click project → **Clean and Build**
3. Right-click project → **Run**
4. API is live at: **http://localhost:8080/api/v1/**

### Run from terminal

```bash
mvn clean package
java -jar target/smart-campus-api-1.0-SNAPSHOT.jar
```

---

## API Overview

Base path: `/api/v1`

| Method | Endpoint                      | Description                               |
| ------ | ----------------------------- | ----------------------------------------- |
| GET    | /api/v1/                      | Discovery — API metadata and links        |
| GET    | /api/v1/rooms                 | List all rooms                            |
| POST   | /api/v1/rooms                 | Create a new room                         |
| GET    | /api/v1/rooms/{id}            | Get a specific room                       |
| DELETE | /api/v1/rooms/{id}            | Delete a room (blocked if sensors exist)  |
| GET    | /api/v1/sensors               | List all sensors (optional ?type= filter) |
| POST   | /api/v1/sensors               | Register a new sensor                     |
| GET    | /api/v1/sensors/{id}          | Get a specific sensor                     |
| DELETE | /api/v1/sensors/{id}          | Delete a sensor                           |
| GET    | /api/v1/sensors/{id}/readings | Get reading history                       |
| POST   | /api/v1/sensors/{id}/readings | Add a new reading                         |

### Error Responses

| Scenario                                  | HTTP Status               |
| ----------------------------------------- | ------------------------- |
| Room deleted while sensors still assigned | 409 Conflict              |
| Sensor created with non-existent roomId   | 422 Unprocessable Entity  |
| Reading posted to MAINTENANCE sensor      | 403 Forbidden             |
| Resource not found                        | 404 Not Found             |
| Any unexpected runtime error              | 500 Internal Server Error |

### Pre-loaded Seed Data

- Rooms: `LIB-301` (Library Quiet Study, cap 50), `CS-101` (Computer Science Lab, cap 30)
- Sensors: `TEMP-001` (Temperature, ACTIVE, LIB-301), `CO2-001` (CO2, ACTIVE, CS-101), `OCC-001` (Occupancy, MAINTENANCE, LIB-301)

---

## Sample curl Commands

> Start the server first. Base URL: `http://localhost:8080/api/v1`

### 1. Discovery

```bash
curl -s http://localhost:8080/api/v1/
```

### 2. List all rooms

```bash
curl -s http://localhost:8080/api/v1/rooms
```

### 3. Create a room

```bash
curl -s -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d "{\"id\":\"ENG-205\",\"name\":\"Engineering Lab\",\"capacity\":35}"
```

### 4. Get a specific room

```bash
curl -s http://localhost:8080/api/v1/rooms/LIB-301
```

### 5. Delete room with sensors — 409 Conflict

```bash
curl -s -X DELETE http://localhost:8080/api/v1/rooms/LIB-301
```

### 6. Delete empty room — 204 No Content

```bash
curl -s -X DELETE http://localhost:8080/api/v1/rooms/ENG-205
```

### 7. List all sensors

```bash
curl -s http://localhost:8080/api/v1/sensors
```

### 8. Filter sensors by type

```bash
curl -s "http://localhost:8080/api/v1/sensors?type=CO2"
```

### 9. Create sensor with valid roomId — 201 Created

```bash
curl -s -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d "{\"id\":\"HUM-001\",\"type\":\"Humidity\",\"status\":\"ACTIVE\",\"currentValue\":55.0,\"roomId\":\"CS-101\"}"
```

### 10. Create sensor with fake roomId — 422 Unprocessable Entity

```bash
curl -s -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d "{\"id\":\"BAD-001\",\"type\":\"CO2\",\"status\":\"ACTIVE\",\"currentValue\":0,\"roomId\":\"FAKE-ROOM\"}"
```

### 11. Post reading to ACTIVE sensor — 201 Created

```bash
curl -s -X POST http://localhost:8080/api/v1/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -d "{\"value\":23.5}"
```

### 12. Post reading to MAINTENANCE sensor — 403 Forbidden

```bash
curl -s -X POST http://localhost:8080/api/v1/sensors/OCC-001/readings \
  -H "Content-Type: application/json" \
  -d "{\"value\":5.0}"
```

### 13. Get reading history

```bash
curl -s http://localhost:8080/api/v1/sensors/TEMP-001/readings
```

### 14. Non-existent resource — 404 Not Found

```bash
curl -s http://localhost:8080/api/v1/rooms/FAKE-999
```

---

## Report — Question Answers

### Part 1.1 — JAX-RS Resource Lifecycle

By default, JAX-RS creates a new resource class instance for every incoming HTTP request (per-request lifecycle). Each request gets its own fresh object, making it thread-safe by nature since no mutable state is shared between threads via instance fields. However, this means any data stored as instance variables would be lost between requests.

To manage in-memory data correctly, this API uses a **singleton `DataStore`** class backed by `ConcurrentHashMap`. Since multiple requests can arrive simultaneously from different threads, a plain `HashMap` would cause race conditions — lost updates and `ConcurrentModificationException`. `ConcurrentHashMap` uses segment-level locking, allowing safe concurrent reads and writes without data loss or corruption across all resource instances.

### Part 1.2 — HATEOAS

HATEOAS is a hallmark of advanced REST because it makes APIs self-describing and self-navigable. Rather than requiring clients to consult external documentation to know what URLs exist, the server embeds navigation links directly in every response — just as a human explores a website by following hyperlinks rather than memorising URLs.

Compared to static documentation: links embedded in responses are always in sync with the running server (documentation goes stale), clients can discover new capabilities at runtime without code changes, URL structures can evolve server-side without breaking clients that follow links, and no separate API portal is needed for basic navigation.

### Part 2.1 — Full Objects vs IDs in List Responses

Returning full room objects costs more bandwidth per call but means clients are immediately ready to render UI without further requests. Returning only IDs produces smaller initial responses but forces the client to issue N follow-up `GET /rooms/{id}` calls — the classic "N+1 problem". For a campus with hundreds of rooms this significantly increases latency and server load. Industry best practice for moderate-sized collections is to return full objects; for very large datasets, use pagination with summary projections.

### Part 2.2 — Idempotency of DELETE

DELETE is idempotent per HTTP/1.1 (RFC 7231): multiple identical requests must leave the server in the same final state as a single request. In this implementation, the first `DELETE /rooms/{id}` removes the room and returns **204 No Content**. Any subsequent identical request finds the room already gone and returns **404 Not Found**. The server state (room absent) is identical after each call — only the response status code differs, which is explicitly permitted by RFC 7231. Idempotency is therefore fully maintained.

### Part 3.1 — @Consumes and Content-Type Mismatch

When `@Consumes(MediaType.APPLICATION_JSON)` is declared on a POST method, JAX-RS inspects the incoming request's `Content-Type` header before invoking any business logic. If a client sends `text/plain` or `application/xml`, the framework finds no resource method whose `@Consumes` annotation matches and automatically returns **HTTP 415 Unsupported Media Type** — the method body never executes. This is enforced entirely by the framework's content negotiation layer, keeping resource methods clean of defensive type-checking code.

### Part 3.2 — Query Parameters vs Path Segments for Filtering

`GET /sensors?type=CO2` is preferred over `GET /sensors/type/CO2` because:

- **Semantic correctness**: Path segments identify discrete resources; query parameters modify or filter a collection. A sensor type is a filter criterion, not a sub-resource identity.
- **Composability**: Multiple filters compose naturally — `?type=CO2&status=ACTIVE` — whereas nested path segments cannot cleanly express multi-criteria filtering without inventing an arbitrary ordering convention.
- **RFC 3986 alignment**: The URI specification reserves hierarchical path segments for resource structure and query strings for parameterisation.
- **HTTP caching**: Caches and proxies distinguish paths from query strings; filter parameters as query strings align with standard HTTP caching semantics.

### Part 4.1 — Sub-Resource Locator Pattern

The Sub-Resource Locator pattern delegates nested-path handling to dedicated resource classes rather than putting every endpoint into one massive controller. Benefits:

- **Single Responsibility**: `SensorResource` handles sensor CRUD; `SensorReadingResource` exclusively manages readings. Each class has one reason to change.
- **Independent testability**: Each sub-resource can be unit-tested in complete isolation.
- **Reduced complexity**: Developers navigate to the correct class for the correct concern rather than scanning hundreds of methods in one file.
- **Open/Closed Principle**: Adding `/alerts` or `/calibrations` sub-resources means adding a new locator method and a new class — existing code is completely untouched.
- **Runtime delegation**: JAX-RS resolves the full path `/sensors/{id}/readings` by calling the locator method and continuing path matching on the returned object, enabling full JAX-RS annotation support on the sub-resource class.

### Part 5.2 — HTTP 422 vs 404 for Missing Referenced Resource

A **404 Not Found** signals that the URL the client requested does not exist. When a client POSTs to `/api/v1/sensors` with a `roomId` that does not exist, the endpoint URL is perfectly valid — the problem is that a value embedded inside the request body references a non-existent entity. **HTTP 422 Unprocessable Entity** (RFC 4918) was designed precisely for this: the request is syntactically well-formed and understood, but it contains a semantic or business-logic error. Using 422 tells client developers "your JSON is valid but its content violates a referential integrity rule" — far more informative and actionable than a generic 404.

### Part 5.4 — Security Risks of Exposing Stack Traces

Exposing raw Java stack traces to API consumers is a significant security risk because they reveal:

1. **Class and package names** — attackers learn internal architecture and can identify specific classes to target.
2. **Library names and versions** — enables attackers to look up known CVEs for exact dependency versions.
3. **Internal file paths** — reveals server directory structure and deployment layout.
4. **Logic flow and failure points** — null pointer locations and method call chains help attackers craft inputs designed to trigger specific code paths.
5. **Framework and server details** — confirms the technology stack, narrowing the attack surface.

The `GlobalExceptionMapper` catches all unhandled `Throwable`s, logs full detail server-side where only administrators can access it, and returns only a generic "An unexpected error occurred" message to the client — eliminating all information leakage.

### Part 5.5 — JAX-RS Filters vs Per-Method Logging

Using a JAX-RS filter (`ApiLoggingFilter`) for logging is superior to inserting `Logger.info()` calls into every resource method because:

- **Consistency**: Logging is guaranteed on every request and cannot be accidentally omitted when new endpoints are added.
- **Single Responsibility**: Resource methods contain only business logic; logging is a cross-cutting concern handled separately.
- **DRY Principle**: Log format changes require editing one class, not dozens of resource methods across the codebase.
- **Non-invasive**: Filters are registered via `@Provider` annotation without modifying any resource class — resource code is completely unaware logging exists.
- **Ordering control**: Multiple filters can be chained with defined execution order, enabling composable concerns like authentication, rate-limiting, and logging in a clean pipeline.
