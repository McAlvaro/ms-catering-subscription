# Unit Testing Rules — ms-catering-subscription

> **Harness de Pruebas Unitarias** para el microservicio de Suscripción y Calendario de Catering.
> Basado en Clean Architecture + DDD. Aplica a todos los módulos: `domain`, `application` e `infrastructure`.

---

## 1. Stack de Testing

| Herramienta | Versión | Propósito |
|---|---|---|
| **JUnit Jupiter** | 5.x (via Spring Boot parent) | Motor de ejecución de tests |
| **AssertJ** | 3.x (via Spring Boot parent) | Aserciones fluidas y expresivas |
| **Mockito** | 5.x (via `mockito-junit-jupiter`) | Creación de mocks y verificación de interacciones |
| **JaCoCo** | 0.8.12 | Medición y reporte de Code Coverage |
| **Maven Surefire** | 3.2.5 | Ejecución de tests en el ciclo de vida de Maven |
| **H2** | Runtime (via Spring Boot parent) | Base de datos en memoria para tests de infraestructura |
| **Spring Boot Test** | 4.x | Context y MockMvc para tests de integración en `infrastructure` |

---

## 2. Estructura de Archivos

Los tests **espejean** la estructura del código fuente. La carpeta de prueba debe replicar exactamente el mismo paquete que la clase que testea.

```
src/
├── main/java/com/mcalvaro/mscatering/
│   └── domain/subscription/vo/ValidityPeriod.java
└── test/java/com/mcalvaro/mscatering/
    └── domain/subscription/vo/ValidityPeriodTest.java   ← mismo paquete
```

**Convención de nombre:** `<NombreDeClase>Test.java`

---

## 3. Reglas por Capa

### 3.1 Capa `domain/`

> Lógica de negocio pura. **Sin Spring, sin Mockito** (salvo fixtures). Solo JUnit 5 + AssertJ.

| Regla | Descripción |
|---|---|
| **R-D01** | Los tests de Value Objects (VOs) deben verificar cada invariante de negocio con un test dedicado. |
| **R-D02** | Usar `@ParameterizedTest` con `@NullAndEmptySource` + `@ValueSource` para cubrir valores nulos, vacíos y en blanco en una sola prueba. |
| **R-D03** | Verificar el código de error (`code`) de `DomainException` con `.hasFieldOrPropertyWithValue("code", "XX-000")`. |
| **R-D04** | Verificar la igualdad por valor de Records con `.isEqualTo()` — no usar `==`. |
| **R-D05** | Los tests de Aggregate Roots deben cubrir: creación (`create`), transiciones de estado, y excepciones de invariantes de dominio. |
| **R-D06** | Prohibido usar `mock()` sobre clases del dominio en esta capa. Los objetos de dominio deben usarse reales. |

**Ejemplo de referencia:** `domain/src/test/java/.../subscription/vo/ValidityPeriodTest.java`

---

### 3.2 Capa `application/`

> Casos de uso / Command Handlers. Toda dependencia externa se **mockea**. Nunca se levanta Spring.

| Regla | Descripción |
|---|---|
| **R-A01** | Toda clase de test debe anotarse con `@ExtendWith(MockitoExtension.class)`. |
| **R-A02** | Las interfaces de repositorio y servicios de dominio se declaran con `@Mock`. |
| **R-A03** | La clase bajo prueba se declara con `@InjectMocks` (Mockito inyecta el constructor). |
| **R-A04** | Usar `when(...).thenReturn(...)` (stubbing) para simular respuestas de colaboradores. |
| **R-A05** | Usar `verify(mock, times(N)).metodo(arg)` para confirmar que el handler llamó a sus colaboradores. |
| **R-A06** | Usar `verify(mock, never()).metodo(any())` para confirmar que **no** se persiste en caso de error. |
| **R-A07** | No llamar a `new` sobre implementaciones de repositorios ni servicios de infraestructura. |
| **R-A08** | El helper de fixture (`buildXxx()`) se declara como `private` al final de la clase. |
| **R-A09** | Se permite `mock()` en fixtures solo si el test no verifica el estado interno de ese objeto. |

**Escenarios mínimos a cubrir en un Command Handler:**

1. ✅ **Happy Path** — el comando se ejecuta correctamente y retorna el resultado esperado.
2. 🔍 **Verificación de contrato** — los argumentos correctos se pasan a los colaboradores.
3. ❌ **Propagación de error** — si el dominio lanza una excepción, el repositorio nunca es llamado.

**Ejemplo de referencia:** `application/src/test/java/.../CloseConsolidatedCalendarCommandHandlerTest.java`

---

### 3.3 Capa `infrastructure/`

> Controladores REST, mappers, repositorios JPA. Se usa Spring Boot Test + H2 cuando se requiere contexto.

| Regla | Descripción |
|---|---|
| **R-I01** | Los tests de controllers REST usan `@WebMvcTest(XController.class)` + `MockMvc`. Nunca `@SpringBootTest` completo. |
| **R-I02** | Los casos de uso de `application` se mockean con `@MockBean` en los tests de controller. |
| **R-I03** | Los tests de repositorios JPA usan `@DataJpaTest` con H2 en memoria. Nunca requieren MySQL activo. |
| **R-I04** | Los tests de Mapper/Converter son tests unitarios puros: sin Spring, sin Mockito. |
| **R-I05** | La base de datos de test debe usar `spring.jpa.hibernate.ddl-auto=create-drop` o Liquibase separado. |

---

## 4. Convenciones de Código

### 4.1 Nomenclatura de métodos

```
should[ResultadoEsperado]When[Condicion]

Ejemplos:
  shouldCreateValidityPeriodSuccessfullyWhenDatesAreValid()
  shouldThrowDomainExceptionWhenStreetIsBlank()
  shouldPropagateExceptionWhenCalendarHasNoLines()
```

### 4.2 Anotacion @DisplayName

- **Obligatoria** en todo método de test.
- **Idioma:** inglés.
- **Formato:** oración completa que describe el comportamiento esperado.

```java
@DisplayName("Should throw DomainException VO-003 when street is null, empty or blank")
```

### 4.3 Aserciones significativas

Toda aserción debe verificar un comportamiento real del sistema. Las aserciones triviales o tautológicas no aportan valor y dan una falsa sensación de cobertura.

```java
// Prohibido — siempre pasa, no verifica nada
assertThat(true).isTrue();
assertTrue(true);

// Correcto — verifica el estado real del objeto bajo prueba
assertThat(calendar.getStatus()).isEqualTo(ConsolidateStatus.CLOSED);
assertThat(returnedId).isEqualTo(calendar.getId());
```

### 4.4 Estructura interna: AAA (Arrange-Act-Assert)

Todo test debe separar visualmente sus tres fases con comentarios:

```java
// Arrange  <- preparar datos y stubs
// Act      <- ejecutar la acción bajo prueba
// Assert   <- verificar el resultado

// Nota: si Act y Assert ocurren juntos, usar:
// Act & Assert
```

Queda **prohibido** capturar excepciones con `try/catch` vacío o sin aserción. Si se espera una excepción, usar `assertThatThrownBy()`:

```java
// Prohibido — el test siempre pasa aunque se lance la excepción equivocada
try {
    handler.handle(command);
} catch (Exception e) { }

// Correcto — verifica tipo y mensaje de la excepción
assertThatThrownBy(() -> handler.handle(command))
        .isInstanceOf(DomainException.class)
        .hasFieldOrPropertyWithValue("code", "CAL-002");
```

### 4.5 Visibilidad de la clase de test

Las clases de test se declaran **sin modificador de acceso** (package-private), no `public`.

```java
// Correcto
class ValidityPeriodTest { ... }

// Incorrecto (innecesario en JUnit 5)
public class ValidityPeriodTest { ... }
```

---

## 5. Code Coverage — Objetivos

| Módulo | Líneas | Ramas |
|---|---|---|
| `domain` | >= 80% | >= 75% |
| `application` | >= 70% | >= 60% |
| `infrastructure` | >= 50% | >= 40% |

### Comandos Maven

```bash
# Ejecutar todos los tests y generar reportes JaCoCo
mvn test

# Ejecutar solo el módulo domain
mvn -pl domain test

# Ejecutar solo el módulo application
mvn -pl application test -am

# Ver reporte de cobertura (abrir en navegador)
xdg-open domain/target/site/jacoco/index.html
xdg-open application/target/site/jacoco/index.html
```

---

## 6. Prohibiciones Globales

| Prohibido | Alternativa |
|---|---|
| `@SpringBootTest` en tests de `domain` o `application` | Tests unitarios puros sin contexto |
| Conexión real a MySQL en tests | H2 en memoria con `@DataJpaTest` |
| `Thread.sleep()` en tests | Inyectar un `Clock` mockeado |
| `@Disabled` sin comentario explicativo | Dejar un TODO con la razón y fecha |
| `System.out.println` en tests | Usar el reporte de Surefire / JaCoCo |
| `assertTrue(true)` o `assertThat(true).isTrue()` | Aserción sobre el estado real del objeto bajo prueba |
| `try/catch` vacío o sin aserción para capturar excepciones | `assertThatThrownBy(() -> ...).isInstanceOf(...)`|
