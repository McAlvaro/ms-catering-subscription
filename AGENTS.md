# AGENTS.md

## Repo Shape
- This is a Maven multi-module project: root `pom.xml` has packaging `pom` and includes
  three modules: `domain`, `application`, and `infrastructure`.
- Java is pinned by the POMs to source/target 21; use a JDK 21 toolchain for local verification.
- Clean Architecture (Hexagonal / Ports & Adapters) with lightweight CQRS via Pipelinr.
- `.github/copilot-instructions.md` describes the intended layering.

## Commands
- Build/verify everything from the repo root with `mvn test` or `mvn verify`.
- Work only on a specific module with `mvn -pl <module> test`; add `-am` if upstream modules are needed.
- Integration tests (Failsafe, `*IT.java`): `mvn -pl infrastructure verify`.
- Consumer contract tests: `mvn -pl infrastructure test -Dtest=SubscriptionConsumerPactTest`.
- Provider contract tests: `mvn -pl infrastructure test -Dtest=SubscriptionProviderPactTest`.
- There is no Maven wrapper checked in, so commands require a system `mvn` installation.

## Domain Notes
- The README describes tactical DDD for BC3, "Suscripción y Calendario de Catering".
- The documented aggregate roots are `Suscripcion` and `CalendarioConsolidado`.
- `PacienteReferencia` is a read model sourced from BC1 patient events, not an aggregate.
- `domain/` must stay pure Java: no Spring, JPA, framework annotations or dependencies.

## Contract Testing (Pact JVM)
- Consumer: `NutricenterPortalConsumer` — models the portal/app client of BC3.
- Provider: `ms-catering-subscription` — BC3 REST API.
- Pact version: `4.6.15` (consumer JUnit 5 + provider JUnit 5 Spring).
- Consumer tests: `infrastructure/src/test/java/.../contract/consumer/`.
- Provider tests: `infrastructure/src/test/java/.../contract/provider/`.
- Generated contracts: `pacts/*.json` (versionados en la raíz del repositorio).
- Rules: `project/contract-testing-rules.md`.
- Skills: `.agents/skills/pact-writer/`, `.agents/skills/pact-verifier/`.
- Agent: `.agents/agents/pact-generator.md`.

## Gotchas
- The Provider Pact Test uses `@SpringBootTest(webEnvironment = RANDOM_PORT)` — NOT MockMvc.
  Do not mix it with `BaseIntegrationTest`.
- Run the Consumer Test **before** the Provider Test so the contract JSON exists.
- Do not add Spring Boot, persistence, messaging, or test framework assumptions to `domain/`.
- Los contratos JSON se generan en `pacts/` en la raíz del repositorio para ser
  versionados en Git y compartidos entre equipos.
