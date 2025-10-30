# 💳 Payment API with Dynamic Webhooks

A simple payment processing API built with Java 24 + Spring Boot 3 + MongoDB. It demonstrates:

- Creating payments with secure storage (card number encrypted at rest)
- Registering dynamic webhooks via API
- Asynchronous, resilient webhook delivery with retries and exponential backoff
- OpenAPI documentation with examples at the project root

## Tech Stack

- Java 24
- Spring Boot 3.3
- MongoDB (Spring Data)
- WebClient (reactive) for webhook POSTs
- springdoc-openapi UI for interactive docs

## Getting Started

1) Prerequisites

- Java 24 installed (set `JAVA_HOME` accordingly)
- MongoDB running locally or a connection URI available

2) Configuration
   Environment variables (or edit `src/main/resources/application.properties`):

- `MONGODB_URI` (default: `mongodb://localhost:27017/paymentdb`)
- `PAYMENT_ENC_SECRET` Base64-encoded AES key (16/24/32 bytes). Example secret included for dev only.

3) Build and Run

- Maven:
    - `./mvnw spring-boot:run` (Linux/macOS)
    - `mvnw.cmd spring-boot:run` (Windows)
- Or package and run:
    - `./mvnw clean package` then `java -jar target/simple-payment-application-0.0.1-SNAPSHOT.jar`

4) API Docs

- OpenAPI file: `openapi.yaml` (at project root)
- Swagger UI (when app is running): http://localhost:8080/swagger-ui/index.html

## Endpoints (Summary)

- POST `/api/payments` → Create a payment (201 Created)
- POST `/api/webhooks` → Register a webhook (201 Created)
- GET  `/api/webhooks` → List active webhooks (200 OK)

## Request/Response Examples

Create payment:

```bash
curl -X POST http://localhost:8080/api/payments \
  -H 'Content-Type: application/json' \
  -d '{
    "firstName": "Jane",
    "lastName": "Doe",
    "zipCode": "94105",
    "cardNumber": "4242424242424242"
  }'
```

Response 201:

```json
{
  "id": "665a2b9f1e2f4c6d8a7b9012",
  "firstName": "Jane",
  "lastName": "Doe",
  "zipCode": "94105",
  "cardLast4": "4242",
  "createdAt": "2025-10-30T15:04:05Z"
}
```

Register webhook:

```bash
curl -X POST http://localhost:8080/api/webhooks \
  -H 'Content-Type: application/json' \
  -d '{"endpointUrl": "https://webhook.site/your-endpoint"}'
```

List active webhooks:

```bash
curl http://localhost:8080/api/webhooks
```

## Security Notes

- Card numbers are never returned by the API. Only `cardLast4` is exposed.
- Card numbers are encrypted at rest using AES-GCM with a secret key provided via env var.
- Do NOT use the example encryption secret in production.

## Resilient Webhooks

- Each new payment enqueues a webhook event per active registration.
- Dispatcher runs periodically and POSTs JSON payloads; non-2xx results are retried with exponential backoff.
- Max attempts and backoff are configurable via properties.

## Configuration Properties

See `src/main/resources/application.properties` for defaults:

- `webhook.dispatch.enabled` (default: true)
- `webhook.dispatch.max-attempts` (default: 8)
- `webhook.dispatch.base-backoff-ms` (default: 2000)
- `webhook.dispatch.max-backoff-ms` (default: 120000)

## OpenAPI Specification

The OpenAPI 3 spec with examples is available in `openapi.yaml` at the project root. Import it into your API client or
open with Swagger UI while the app is running.
