# Actividad: Continuous Integration inicial (Módulo 6 CI/CD)

Plan de adaptación del taller de Continuous Integration (clases 01–03, stack .NET/React) al stack de BC3: **Java 21 + Spring Boot + Maven multi-módulo**.

## Enunciado (resumen)

- Configurar Husky y lint-staged en el repositorio.
- Forzar el análisis con la herramienta de linting/code formatting antes del commit.
- Crear el ciclo de CI en GitHub Actions con los comandos de compilación.
- Agregar el escaneo básico de SonarCloud.
- Presentar el enlace al repositorio.
- Si se usó IA, subir skills, scripts, `CLAUDE.md` o `AGENTS.md` correspondientes.

## Mapeo de requisitos al stack Java/Spring Boot

| # | Requisito | En la clase (.NET/React) | En este proyecto (Java 21 + Maven multi-módulo) |
|---|---|---|---|
| 1 | Herramienta de linting/formateo | Prettier, `dotnet format`, `.editorconfig` | **Spotless Maven Plugin** (`spotless:apply` / `spotless:check`) con formateador Java + `.editorconfig` |
| 2 | Husky + lint-staged | `pnpm init` en la raíz + Husky | Igual: `package.json` mínimo en la raíz (Node solo para tooling), Husky con `"prepare": "husky"` |
| 3 | Forzar análisis antes del commit | `pre-commit` → lint-staged → `dotnet format --include` | `pre-commit` → lint-staged → `mvn spotless:apply -DspotlessFiles=<staged>` (solo `.java` staged) |
| 3b | (Clase) `pre-push` con tests | `dotnet test` | `mvn -B -q spotless:check test` (formato de todo el repo + unit tests; atrapa commits hechos con `--no-verify`; IT y Pact quedan para CI) |
| 3c | (Clase 02, opcional) commitlint | `commit-msg` | Igual; el repositorio ya usa Conventional Commits, solo se hace cumplir |
| 4 | CI en GitHub Actions | restore → build → test | `.github/workflows/ci.yml` en push y PR a `main` (`actions/setup-java@v6`, temurin 21, cache maven): job `format` (`spotless:check`) → en paralelo `build-test` (`mvn -B -ntp verify` + reporte JaCoCo + análisis SonarCloud) y `contract-tests` (Pact consumer → `git diff pacts/` → provider) |
| 5 | SonarCloud básico | scanner + `SONAR_TOKEN` + `fetch-depth: 0` | `mvn verify org.sonarsource.scanner.maven:sonar-maven-plugin:sonar` dentro del job `build-test`, con cobertura JaCoCo (XML por módulo) |
| 6 | Enlace al repositorio | — | <https://github.com/McAlvaro/ms-catering-subscription> (público) |
| 7 | Artefactos de IA | `agents.md`, skills | Actualizar `AGENTS.md` / `CLAUDE.md` con hooks y flujo CI; versionar `.agents/` |

## Consideraciones

- **Commit de formateo base:** el primer `spotless:apply` reformatea todo el código; se hace en un commit aislado (`style: apply spotless baseline`) para no mezclarlo con cambios funcionales.
- **Pasos manuales en SonarCloud:** crear organización y proyecto en sonarcloud.io, desactivar *Automatic Analysis* (si no, el coverage aparece en 0%) y agregar `SONAR_TOKEN` como secret del repositorio.
- **Otros repositorios del grupo:** este repositorio corresponde solo a BC3; la misma configuración debe replicarse en cada microservicio del grupo.

## Orden de implementación

1. Spotless + `.editorconfig` + commit de formateo base.
2. Husky + lint-staged (`pre-commit`) + `pre-push` + commitlint.
3. Workflow de CI en GitHub Actions.
4. SonarCloud.
5. Documentación y artefactos de IA.

## Estado final

| Fila | Estado | Commits |
|---|---|---|
| 1 | Completada | `5049a34` build: add spotless with palantir-java-format; `584c805` style: apply spotless baseline; `4ff1377` chore: ignore spotless baseline in git blame |
| 2, 3, 3b, 3c | Completadas | `c07d387` build(hooks): add husky, lint-staged and commitlint tooling; `dc7f032` build(hooks): format staged java files with spotless on pre-commit; `693d971` build(hooks): lint commit messages with commitlint; `277d7f4` build(hooks): run spotless check and unit tests on pre-push |
| 4 | Completada | `ad3aa14` ci: add github actions workflow for format, build/test and pact contracts; correcciones de IT detectadas por CI: `f4fb5ee` test(it): isolate committed data and remove fixed contract codes from IT fixtures, `0b18e68` test(it): merge @AfterEach methods in SaveDomainEventToOutboxHandlerIT |
| 5 | Completada | `26af451` build(jacoco): include failsafe integration tests in coverage report; `1ded0d8` build(sonar): configure sonar-maven-plugin and sonarcloud project properties; `b32e79b` ci: run sonarcloud analysis in build-test job |
| 6 | Completada | Repositorio: <https://github.com/McAlvaro/ms-catering-subscription>; PR: <https://github.com/McAlvaro/ms-catering-subscription/pull/1> |
| 7 | Completada | `0f96bf1` docs(readme): document ci, git hooks and sonarcloud; `f3ad864` docs(agents): document hooks, ci and sonarcloud for agents; `docs: add CI activity plan` (este documento) |

## Enlaces

- SonarCloud: <https://sonarcloud.io/project/overview?id=McAlvaro_ms-catering-subscription>

## Uso de IA

Se utilizó IA (Claude Code) con un flujo de especificación, aprobación humana, implementación guiada por TDD y verificación. Los artefactos y el detalle del flujo están en la sección [4.7 del README](../README.md#47-uso-de-ia-y-artefactos).
