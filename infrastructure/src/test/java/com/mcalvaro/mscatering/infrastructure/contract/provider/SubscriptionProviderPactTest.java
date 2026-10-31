package com.mcalvaro.mscatering.infrastructure.contract.provider;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import au.com.dius.pact.provider.spring.junit5.PactVerificationSpringProvider;
import com.mcalvaro.mscatering.domain.core.id.AddressId;
import com.mcalvaro.mscatering.domain.core.id.DietPlanId;
import com.mcalvaro.mscatering.domain.core.id.EvaluationId;
import com.mcalvaro.mscatering.domain.core.id.PatientId;
import com.mcalvaro.mscatering.domain.core.id.SubscriptionId;
import com.mcalvaro.mscatering.domain.subscription.Subscription;
import com.mcalvaro.mscatering.domain.subscription.entity.BiweeklyEvaluation;
import com.mcalvaro.mscatering.domain.subscription.enums.ServiceType;
import com.mcalvaro.mscatering.domain.subscription.vo.DeliveryAddress;
import com.mcalvaro.mscatering.domain.subscription.vo.DeliveryPreferences;
import com.mcalvaro.mscatering.domain.subscription.vo.ServiceContract;
import com.mcalvaro.mscatering.domain.subscription.vo.TimeWindow;
import com.mcalvaro.mscatering.domain.subscription.vo.ValidityPeriod;
import com.mcalvaro.mscatering.infrastructure.MsCateringApplication;
import com.mcalvaro.mscatering.infrastructure.persistence.subscription.entity.SubscriptionJpaEntity;
import com.mcalvaro.mscatering.infrastructure.persistence.subscription.mapper.SubscriptionMapper;
import com.mcalvaro.mscatering.infrastructure.persistence.subscription.repository.SpringDataSubscriptionRepository;
import com.mcalvaro.mscatering.infrastructure.persistence.patient.entity.PatientReferenceJpaEntity;
import com.mcalvaro.mscatering.infrastructure.persistence.patient.repository.SpringDataPatientReferenceRepository;
import com.mcalvaro.mscatering.infrastructure.persistence.patientaddress.entity.PatientAddressReferenceJpaEntity;
import com.mcalvaro.mscatering.infrastructure.persistence.patientaddress.repository.SpringDataPatientAddressReferenceRepository;
import com.mcalvaro.mscatering.infrastructure.integration.inbound.bc2.dietplan.DietPlanInboundAdapter;
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
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.mock.web.MockHttpServletRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import java.util.List;

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
 * <li>POST /api/subscriptions → 201 Created con UUID</li>
 * <li>GET /api/subscriptions/{id} → 200 OK con DTO de suscripción</li>
 * </ol>
 */
@Provider("ms-catering-subscription")
@PactFolder("../pacts")
@SpringBootTest(classes = MsCateringApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SubscriptionProviderPactTest {

    @LocalServerPort
    private int port;

    @Autowired
    private SpringDataPatientReferenceRepository patientReferenceRepository;

    @Autowired
    private SpringDataPatientAddressReferenceRepository addressReferenceRepository;

    /**
     * Repositorio Spring Data directo para suscripciones: evita la llamada al
     * {@code SpringDomainEventDispatcher} que es request-scoped.
     */
    @Autowired
    private SpringDataSubscriptionRepository springDataSubscriptionRepository;

    @Autowired
    private SubscriptionMapper subscriptionMapper;

    @Autowired
    private DietPlanInboundAdapter dietPlanInboundAdapter;

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
    @State("a patient with ID PAC-550e8400-e29b-41d4-a716-446655440000 exists and owns address DIR-770e8400-e29b-41d4-a716-446655440010")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Rollback(false)
    void patientExistsAndIsEligible() {
        String patientId = "PAC-550e8400-e29b-41d4-a716-446655440000";
        savePatientAndAddress(patientId, "DIR-770e8400-e29b-41d4-a716-446655440010");
        ingestPactPlan("PLAN-660e8400-e29b-41d4-a716-446655440001", LocalDate.of(2026, 10, 1));
    }

    /**
     * Estado requerido para Interacción 2 (GET /api/subscriptions/{id}).
     * Persiste una suscripción activa con el ID fijo del contrato.
     */
    @State("a subscription with ID SUS-770e8400-e29b-41d4-a716-446655440002 exists")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Rollback(false)
    void subscriptionExistsInRepository() {
        SubscriptionId subscriptionId = SubscriptionId.of("SUS-770e8400-e29b-41d4-a716-446655440002");
        PatientId patientId = PatientId.of("PAC-550e8400-e29b-41d4-a716-446655440000");
        DietPlanId dietPlanId = DietPlanId.of("PLAN-660e8400-e29b-41d4-a716-446655440001");

        savePatientAndAddress(patientId.value(), "DIR-770e8400-e29b-41d4-a716-446655440010");

        if (!springDataSubscriptionRepository.existsById(subscriptionId.value())) {
            LocalDate startDate = LocalDate.now().plusDays(1);
            ServiceContract contract = new ServiceContract(
                    dietPlanId,
                    com.mcalvaro.mscatering.domain.dietplan.PlanType.BASE,
                    new ValidityPeriod(startDate, startDate.plusDays(14)),
                    ServiceType.LUNCH,
                    new BigDecimal("250.00"),
                    "Acepto los terminos y condiciones del servicio de catering.",
                    Instant.now());

            DeliveryPreferences preferences = new DeliveryPreferences(
                    new DeliveryAddress(
                            AddressId.of("DIR-770e8400-e29b-41d4-a716-446655440010"),
                            "Home", "Av. Siempre Viva", "742", "Lima",
                            "Frente al parque", -12.046374, -77.042793, "+51 999 888 777"),
                    new TimeWindow(LocalTime.of(12, 0), LocalTime.of(14, 0)),
                    "Sin picante");

            Subscription subscription = Subscription.create(
                    subscriptionId, patientId, dietPlanId, contract, preferences, 99);

            BiweeklyEvaluation evaluation = new BiweeklyEvaluation(
                    EvaluationId.of("EVAL-" + UUID.randomUUID()), patientId, 1, startDate.plusDays(14));
            subscription.scheduleEvaluations(java.util.List.of(evaluation));

            SubscriptionJpaEntity jpaEntity = subscriptionMapper.toJpaEntity(subscription);
            springDataSubscriptionRepository.saveAndFlush(jpaEntity);
        }
    }

    private void savePatientAndAddress(String patientId, String addressId) {
        if (!patientReferenceRepository.existsById(patientId)) {
            PatientReferenceJpaEntity patient = new PatientReferenceJpaEntity();
            patient.setPatientId(patientId);
            patient.setPatientName("Pact patient");
            patient.setActive(true);
            patient.setUpdatedAt(Instant.now());
            patientReferenceRepository.saveAndFlush(patient);
        }
        if (!addressReferenceRepository.existsById(addressId)) {
            addressReferenceRepository.saveAndFlush(new PatientAddressReferenceJpaEntity(addressId, patientId, "Home",
                    "Av. Siempre Viva", "Frente al parque", "Lima", -12.046374, -77.042793));
        }
    }

    private void ingestPactPlan(String planId, LocalDate start) {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        try {
            dietPlanInboundAdapter.receiveBase(new DietPlanInboundAdapter.DietPlanCreatedEvent(UUID.randomUUID(),
                    "bc2.PlanBaseCreado", "1.0", Instant.now(), "MSNutritionPlanner", "pact-plan-" + UUID.randomUUID(),
                    new DietPlanInboundAdapter.DietPlanData(planId, "ACTIVO", start, 15, true,
                            java.util.stream.IntStream.rangeClosed(1, 15).mapToObj(day ->
                                    new DietPlanInboundAdapter.DietPlanDayData(UUID.randomUUID().toString(), day, 1,
                                            List.of(new DietPlanInboundAdapter.DietPlanMealData(UUID.randomUUID().toString(), com.mcalvaro.mscatering.domain.dietplan.MealType.LUNCH, "12:00",
                                                    List.of(new DietPlanInboundAdapter.DietPlanRecipeData("REC-" + UUID.randomUUID(), "Pact recipe", 100)))))).toList(),
                            null, null, null)));
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }
}
