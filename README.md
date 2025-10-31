# 💳 Payment API with Dynamic Webhooks

A simple payment processing API built with Java 24 + Spring Boot 3.3 + MongoDB. It demonstrates:

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

1. **Prerequisites**

    - Java 24 installed (set `JAVA_HOME` accordingly)
    - MongoDB running locally or a connection URI available

2. **Configuration**
   Environment variables (or edit `src/main/resources/application.properties`):

    - `MONGODB_URI` (default: `mongodb://admin:admin123@localhost:27017/paymentsdb?authSource=admin`) – The MongoDB
      connection URI. Ensure your MongoDB user has the appropriate permissions.
    - `PAYMENT_ENC_SECRET` – Base64-encoded AES key (16/24/32 bytes).  **For development only!**  For production,
      generate a strong, randomly-generated key and securely store it – consider using a secrets management solution.
      Example secret included for dev only.
    - `SPRING_CLOUD_STREAM_PAYMENT_EVENTS` –  (Optional)  If you plan to add asynchronous event processing, define a
      streaming configuration here.

3. **Build and Run**

    - Maven:
        - `./mvnw spring-boot:run` (Linux/macOS)
        - `mvnw.cmd spring-boot:run` (Windows)
    - Or package and run:
        - `./mvnw clean package` then `java -jar target/simple-payment-application-0.0.1-SNAPSHOT.jar`

4. **API Docs**

    - OpenAPI file: `openapi.yaml` (at project root)
    - Swagger UI (when app is running): http://localhost:8080/swagger-ui/index.html

## Endpoints (Summary)

| Method    | Endpoint           | Description                               |
| :-------- | :----------------- | :--------------------------------------- |
| POST      | `/api/payments`     | Create a payment                         |
| POST      | `/api/webhooks`     | Register a webhook endpoint              |
| GET       | `/api/webhooks`     | List active webhooks                      |

## Request/Response Examples

**Create Payment:**
```bash
curl -X POST http://localhost:8080/api/payments
-H 'Content-Type: application/json'
-d '{ "firstName": "Jane", "lastName": "Doe", "zipCode": "94105", "cardNumber": "4242424242424242" }'
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

**Register Webhook:**
```bash
curl -X POST http://localhost:8080/api/webhooks
-H 'Content-Type: application/json'
-d '{"endpointUrl": "https://webhook.site/your-endpoint"}'
```

**List Active Webhooks:**
```bash
curl http://localhost:8080/api/webhooks
```

## Webhook Payload Format

Each registered webhook endpoint will receive a JSON payload with the following structure:

```json
{
  "paymentId": "665a2b9f1e2f4c6d8a7b9012",
  "amount": 100.00,
  "currency": "USD",
  "status": "CREATED"
}
```

## Resilient Webhooks

- Each new payment enqueues a webhook event per active registration.
- Dispatcher runs periodically and POSTs JSON payloads; non-2xx results are retried with exponential backoff.
- Max attempts and backoff are configurable via properties.

## Configuration Properties

| Property                           | Default Value | Description                                                 |
|:-----------------------------------|:--------------|:------------------------------------------------------------|
| `webhook.dispatch.enabled`         | `true`        | Enables/disables the webhook dispatcher.                    |
| `webhook.dispatch.max-attempts`    | `8`           | Maximum number of retry attempts.                           |
| `webhook.dispatch.base-backoff-ms` | `2000`        | Initial backoff time in milliseconds.                       |
| `webhook.dispatch.max-backoff-ms`  | `120000`      | Maximum backoff time in milliseconds.                       |
| `spring.threads.virtual.enabled`   | `true`        | Enable virtual threads. Useful for non-blocking operations. |

## Security Notes

- Card numbers are never returned by the API. Only `cardLast4` is exposed.
- Card numbers are encrypted at rest using AES-GCM with a secret key provided via env var.
- Do NOT use the example encryption secret in production.
- Follow best practices for secure secret management.

## Testing

The project includes JUnit tests. To run them, use the command

```bash
./mvnw test
```
