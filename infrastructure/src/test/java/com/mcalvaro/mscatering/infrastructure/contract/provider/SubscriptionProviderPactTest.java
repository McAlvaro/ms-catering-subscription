package com.mcalvaro.mscatering.infrastructure.contract.provider;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import au.com.dius.pact.provider.spring.junit5.PactVerificationSpringProvider;
import com.mcalvaro.mscatering.domain.subscription.Subscription;
import com.mcalvaro.mscatering.domain.subscription.entity.BiweeklyEvaluation;
import com.mcalvaro.mscatering.domain.subscription.enums.ServiceType;
import com.mcalvaro.mscatering.domain.subscription.vo.DeliveryAddress;
import com.mcalvaro.mscatering.domain.subscription.vo.DeliveryPreferences;
import com.mcalvaro.mscatering.domain.subscription.vo.ServiceContract;
import com.mcalvaro.mscatering.domain.subscription.vo.TimeWindow;
import com.mcalvaro.mscatering.domain.subscription.vo.ValidityPeriod;
import com.mcalvaro.mscatering.infrastructure.MsCateringApplication;
import com.mcalvaro.mscatering.infrastructure.persistence.patient.entity.PatientReferenceJpaEntity;
import com.mcalvaro.mscatering.infrastructure.persistence.patient.repository.SpringDataPatientReferenceRepository;
import com.mcalvaro.mscatering.infrastructure.persistence.subscription.entity.SubscriptionJpaEntity;
import com.mcalvaro.mscatering.infrastructure.persistence.subscription.mapper.SubscriptionMapper;
import com.mcalvaro.mscatering.infrastructure.persistence.subscription.repository.SpringDataSubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Prueba de Contrato — Lado del PROVEEDOR.
 * <p>
 * Levanta el contexto completo de Spring Boot (H2 en memoria + Liquibase) y
 * verifica que "ms-catering-subscription" (BC3) satisfaga el contrato generado
 * por el consumidor "NutricenterPortalConsumer".
 * <p>
 * Pact lee el contrato JSON desde {@code target/pacts/} y ejecuta cada
 * interacción pactada contra el servidor HTTP real (RANDOM_PORT).
 * <p>
 * Cada método {@code @State} usa {@code REQUIRES_NEW + @Rollback(false)} para
 * que los datos queden comprometidos antes de que el servidor HTTP atienda
 * la petición Pact. Se usan repositorios Spring Data directos para evitar
 * el {@code SpringDomainEventDispatcher} que es request-scoped.
 * <p>
 * Interacciones verificadas:
 * <ol>
 *   <li>POST /api/subscriptions → 201 Created con UUID</li>
 *   <li>GET /api/subscriptions/{id} → 200 OK con DTO de suscripción</li>
 * </ol>
 */
@Provider("ms-catering-subscription")
@PactFolder("../pacts")
@SpringBootTest(
        classes = MsCateringApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SubscriptionProviderPactTest {

    @LocalServerPort
    private int port;

    /**
     * Spring Data repository para pacientes — permite saveAndFlush para garantizar
     * el commit inmediato e idempotente (upsert mediante merge).
     */
    @Autowired
    private SpringDataPatientReferenceRepository springDataPatientRepository;

    /**
     * Repositorio Spring Data directo para suscripciones: evita la llamada al
     * {@code SpringDomainEventDispatcher} que es request-scoped.
     */
    @Autowired
    private SpringDataSubscriptionRepository springDataSubscriptionRepository;

    @Autowired
    private SubscriptionMapper subscriptionMapper;

    @BeforeEach
    void setUp(PactVerificationContext context) {
        context.setTarget(new HttpTestTarget("localhost", port));
    }

    @TestTemplate
    @ExtendWith(PactVerificationSpringProvider.class)
    void verifyPact(PactVerificationContext context) {
        context.verifyInteraction();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // States: persisten datos en H2 que son visibles al servidor HTTP real.
    // Se usa REQUIRES_NEW + @Rollback(false) para que el commit sea inmediato.
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Estado requerido para Interacción 1 (POST /api/subscriptions).
     * Asegura que el paciente con UUID fijo del contrato exista y esté activo.
     */
    @State("a patient with ID 550e8400-e29b-41d4-a716-446655440000 exists and is eligible")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Rollback(false)
    void patientExistsAndIsEligible() {
        UUID patientId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        PatientReferenceJpaEntity entity = new PatientReferenceJpaEntity();
        entity.setPatientId(patientId);
        entity.setActive(true);
        entity.setUpdatedAt(Instant.now());
        springDataPatientRepository.saveAndFlush(entity);
    }

    /**
     * Estado requerido para Interacción 2 (GET /api/subscriptions/{id}).
     * Persiste una suscripción activa con el UUID fijo del contrato.
     */
    @State("a subscription with ID 770e8400-e29b-41d4-a716-446655440002 exists")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Rollback(false)
    void subscriptionExistsInRepository() {
        UUID subscriptionId = UUID.fromString("770e8400-e29b-41d4-a716-446655440002");
        UUID patientId      = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        UUID dietPlanId     = UUID.fromString("660e8400-e29b-41d4-a716-446655440001");

        // Upsert el paciente (puede que ya exista del primer @State)
        PatientReferenceJpaEntity patientEntity = new PatientReferenceJpaEntity();
        patientEntity.setPatientId(patientId);
        patientEntity.setActive(true);
        patientEntity.setUpdatedAt(Instant.now());
        springDataPatientRepository.saveAndFlush(patientEntity);

        if (!springDataSubscriptionRepository.existsById(subscriptionId)) {
            LocalDate startDate = LocalDate.now().plusDays(1);
            ServiceContract contract = new ServiceContract(
                    dietPlanId,
                    new ValidityPeriod(startDate, startDate.plusDays(14)),
                    ServiceType.LUNCH,
                    new BigDecimal("250.00"),
                    "Acepto los terminos y condiciones del servicio de catering.",
                    Instant.now());

            DeliveryPreferences preferences = new DeliveryPreferences(
                    new DeliveryAddress(
                            "Av. Siempre Viva", "742", "Lima",
                            "Frente al parque", -12.046374, -77.042793, "+51 999 888 777"),
                    new TimeWindow(LocalTime.of(12, 0), LocalTime.of(14, 0)),
                    "Sin picante");

            Subscription subscription = Subscription.create(
                    subscriptionId, patientId, dietPlanId, contract, preferences, 99);

            BiweeklyEvaluation evaluation = new BiweeklyEvaluation(
                    UUID.randomUUID(), patientId, 1, startDate.plusDays(14));
            subscription.scheduleEvaluations(java.util.List.of(evaluation));

            SubscriptionJpaEntity jpaEntity = subscriptionMapper.toJpaEntity(subscription);
            springDataSubscriptionRepository.saveAndFlush(jpaEntity);
        }
    }
}
