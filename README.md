# CertiFreight

An enterprise-grade, high-concurrency B2B logistics platform built with a zero-trust multi-tenant data isolation architecture.

## Architectural Highlights (Backend)

- **Java 21 & Spring Boot 4**: Leveraging the latest JVM optimization matrices and lightweight container initialization rules.
- **Native Data Isolation (Hibernate `@TenantId`)**: Implemented framework-level SQL interception via a custom `CurrentTenantIdentifierResolver`, forcing PostgreSQL 17 to natively scope all operations to the execution thread's context without manual query mapping.
- **Cryptographic Token Verification**: Built a stateless authentication pipeline leveraging `OncePerRequestFilter` and the modern, immutable **JJWT 0.12+ API** to securely unpack and validate signed multi-tenant claims via bearer tokens.
- **Defensive Error API Compliance (RFC 7807)**: Integrated a global `@RestControllerAdvice` execution barrier translating thread state validation failures into standardized, machine-readable Problem Details JSON payloads.
- **Immutable Infrastructure**: Managed local development topologies through Docker Compose configurations pinned to PostgreSQL 17 engine layers.

## Local Development Lifecycle

### Prerequisites
- Java 21 JDK
- Maven 3.9+
- Docker Engine / Docker Desktop

### Database Spin-up
```bash
docker compose up -d