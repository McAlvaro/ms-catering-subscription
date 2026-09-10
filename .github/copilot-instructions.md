# Reglas de Desarrollo: ms-catering-subscription

El objetivo de este proyecto es implementar DDD y Clean Architecture de forma estricta.

## Capas y Dependencias

1. **domain/**:
   - Lógica de negocio pura.
   - CERO dependencias de frameworks (sin Spring, sin JPA).
   - Entidades (usar métodos de fábrica `create`) y Value Objects inmutables.
   - Interfaces para Repositorios.

2. **application/**:
   - Casos de uso (orquestadores).

3. **infrastructure/**:
   - Spring Boot, implementaciones JPA, controladores.
   - Única capa donde se permiten anotaciones de framework.

## Directrices para el Agente de IA

- Antes de generar código, identifica la capa.
- Si generas código para `domain/`, asegúrate de que sea 100% Java estándar.
- No añadas librerías al `pom.xml` a menos que sea estrictamente necesario.
