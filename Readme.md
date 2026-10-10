# Linklet

Linklet is a URL-shortening web application built with Spring Boot. It supports creating short links, optional custom aliases and expiration times, and a dashboard for viewing click analytics.

## Features

- Create short links from a web form or the REST API.
- Optionally assign a custom alphanumeric alias.
- Set an expiration time for a link.
- Redirect active links and return appropriate responses for missing or expired links.
- Store URL records in PostgreSQL and use Redis to cache URL mappings (configured with a five-minute cache TTL).
- Publish click-event details to Kafka for background processing and persistence.
- View click analytics, including total clicks, clicks today, distinct IPs, a seven-day trend, and recent click records.
- Apply configurable IP-based request limiting.
- Run a scheduled cleanup for expired links.

## Technology stack

- **Language/runtime:** Java 17+
- **Backend:** Spring Boot 4
- **Web UI:** Thymeleaf, HTML, CSS, JavaScript
- **Persistence:** PostgreSQL, Spring Data JPA/Hibernate
- **Caching:** Redis
- **Event processing:** Apache Kafka (KRaft mode in the local Compose setup)
- **Testing/build:** Maven Wrapper, JUnit, Mockito, JaCoCo
- **Local infrastructure:** Docker Compose

## Run locally

### Prerequisites

- Java 17+
- Docker Desktop with Docker Compose

### 1. Start PostgreSQL, Redis, and Kafka

```powershell
docker compose up -d
```

The current local configuration maps PostgreSQL to port `5431`, Redis to `6379`, and Kafka to `9092`.

### 2. Start the application

On Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

On macOS/Linux:

```bash
./mvnw spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080).

### 3. Stop the local infrastructure

```powershell
docker compose down
```

This command stops the containers but preserves named volumes. Avoid `docker compose down -v` unless you intentionally want to remove their stored data.

## REST API

### Create a short URL

`POST /api/urls`

Example request:

```json
{
  "longUrl": "https://example.com/article",
  "customAlias": "mylink",
  "expirationTime": "2026-12-31T23:59:59"
}
```

`customAlias` and `expirationTime` are optional. The API returns a URL response containing the short URL and short code.

### Redirect using a short code

`GET /api/urls/{shortCode}`

The current controller returns:

- `302 Found` for a resolvable, active link.
- `404 Not Found` if the short code cannot be found.
- `410 Gone` if the link has expired.

Successful redirects also publish a click event to Kafka for background analytics processing.

## Testing

Run the test suite with:

On Windows:

```powershell
.\mvnw.cmd test
```

On macOS/Linux:

```bash
./mvnw test
```

The project includes tests for controller behavior, URL service logic, analytics processing, and cleanup behavior. The JaCoCo coverage gate is configured separately in the Maven `verify` lifecycle.

