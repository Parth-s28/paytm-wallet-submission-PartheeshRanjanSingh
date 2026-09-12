# Paytm Wallet Assignment

## Current milestone
This starter implements the first milestone:
- Java 21 + Spring Boot
- PostgreSQL + Flyway
- Race-free wallet get-or-create
- GET wallet
- Docker Compose
- Non-root Docker container + healthcheck
- Correlation ID

Transfer implementation and concurrency burst scripts are intentionally the next milestone.

## Run
```bash
docker compose up --build
```

Health:
```bash
curl http://localhost:8080/actuator/health
```

Create/get wallet:
```bash
curl -X POST -H 'Authorization: Bearer alice' http://localhost:8080/wallets
```

Repeat the same request concurrently: every response must contain the same wallet id.
