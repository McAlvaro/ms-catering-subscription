# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

BC3 ("Suscripción y Calendario de Catering") of a DDD course project: Spring Boot microservice, Java 21, Maven multi-module. Aggregate roots: `Suscripcion` and `CalendarioConsolidado`. `PacienteReferencia` and the BC2 diet-plan projection are read models fed by inbound events, not aggregates. Domain language and docs (`README.md`, `docs/`) are largely Spanish; code and identifiers are English-ish/mixed.

## Commands

No Maven wrapper; use system `mvn` with JDK 21.

```bash
mvn clean verify                                   # everything (unit + IT + Pact + JaCoCo)
mvn -pl domain test                                # one module (add -am if upstream modules are needed)
mvn -pl infrastructure test -Dtest=SomeTest        # single unit test
mvn -pl infrastructure verify -Dit.test=CreateSubscriptionApiIT   # single integration test (Failsafe, *IT.java)
mvn -pl infrastructure test -Dtest=SubscriptionConsumerPactTest   # Pact consumer: run BEFORE provider
mvn -pl infrastructure test -Dtest=SubscriptionProviderPactTest   # Pact provider
mvn spotless:apply                                 # format (palantir-java-format); spotless:check to verify
mvn -B -ntp spotless:check verify                  # CI-equivalent run
npm install                                        # once per clone/worktree: installs Husky hooks (Node >= 22.22.1)
```

JaCoCo reports: `<module>/target/site/jacoco/index.html` (generated at `verify`; 80% is a target, not enforced).

Run locally with the `local` profile (`application-local.yml`: port 8081, Eureka/discovery disabled, Swagger UI at `/swagger-ui.html`). Default datasource is MySQL on `127.0.0.1:3325`; `docker-compose.yml` + `.env` (copy from `.env.example`) bring up MySQL and the app image. Tests use H2 with real Liquibase migrations.

## Architecture

Modules: `domain` → `integration-contracts` → `application` → `infrastructure`.

- **domain/** — pure Java. No Spring, JPA, or framework annotations/dependencies. Entities use `create` factory methods; value objects immutable; repository interfaces live here. Strongly typed, prefixed, compact (8-char) identifiers in `domain/core/id`.
- **integration-contracts/** — outbound `IntegrationEvent` classes (v1) plus JSON schemas in `src/test/resources/schemas/v1/` validated by tests. These are the frozen wire contracts to other bounded contexts.
- **application/** — lightweight CQRS via Pipelinr: one package per use case (`subscription/CreateSubscription`, `consolidatedcalendar/CloseConsolidatedCalendar`, ...) with command/query + handler. Also holds the outbox model/ports and inbound-event synchronization services (BC1 patients/addresses, BC2 diet plans).
- **infrastructure/** — the only layer with framework code: REST controllers (`api/`), JPA persistence, Liquibase changelogs (`src/main/resources/db/`), Kafka inbound consumers (`integration/inbound/bc1|bc2|...`), outbox relay, Pipelinr config.

### Request / event flow (needs several files to see)

- Commands pass through Pipelinr with `TransactionalMiddleware` (`infrastructure/config`), so the aggregate change and its outbox row commit in one transaction.
- **Transactional Outbox:** Aggregate → `DomainEvent` → `OutboxMessage` (persisted by `SaveDomainEventToOutboxHandler` via `SpringDomainEventDispatcher`) → `OutboxRelay` polls → `OutboxEventTranslatorRegistry` picks a translator by `supports()` and builds the `IntegrationEvent` **from the persisted snapshot, never from current DB state** → publisher (Kafka). A DomainEvent is never published directly; an IntegrationEvent is never stored in the outbox. Only six domain events are outbound; internal events create no outbox rows. Design and ticket backlog (OUT-001…OUT-009, in progress): `docs/outbox-transaccional-bc3.md`, `docs/outbox/tickets-implementacion.md`.
- **Inbound:** Kafka consumers (BC1 patients/addresses, BC2 diet plans, BC5 delivery) feed local projections; the consolidated-calendar close resolves recipes from the local immutable BC2 plan projection. Delivery address must be selected explicitly (`deliveryAddressId`) — no free-address constructors.

### Runtime config (`infrastructure/src/main/resources/application.yml`)

Kafka is off by default (`KAFKA_ENABLED=false`); the outbox relay follows it (`OUTBOX_RELAY_ENABLED`) and needs a real publisher. Topics/groups, Eureka (`EUREKA_SERVER_URL`, etc.), and relay tuning (poll, batch, lease, backoff) are env-driven.

## Gotchas

- Provider Pact test uses `@SpringBootTest(webEnvironment = RANDOM_PORT)`, NOT MockMvc; do not combine with `BaseIntegrationTest`.
- Generated Pact contracts go to `pacts/` at the repo root and are versioned in Git.
- Testing conventions: `project/unit-testing-rules.md`, `project/integration-test-rules.md`, `project/contract-testing-rules.md`; Pact skills/agent under `.agents/`.
- Before adding code, identify the layer; do not add libraries to a `pom.xml` unless strictly necessary (`.github/copilot-instructions.md`).
- Hooks need `mvn` on `PATH` and `npm install` run in each clone/worktree.
- Do not use `--no-verify` or `HUSKY=0` unless the user asks; CI repeats the checks.
- JaCoCo reports exist only after `mvn verify` (not `mvn test`).
- Pact: the CI fails if the regenerated `pacts/` differ from the committed ones.
- Conventional Commits are enforced by commitlint; CI/hooks/Sonar details are in `AGENTS.md`.
