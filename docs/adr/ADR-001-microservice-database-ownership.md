# ADR-001: Microservice Database Ownership

## Status
Accepted

## Decision
Each microservice owns its own database.

## Rationale
To ensure loose coupling, independent deployments, scalability, and service autonomy.

## Consequences
- Better isolation
- Easier scaling
- More complex cross-service communication