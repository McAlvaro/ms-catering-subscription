package com.mcalvaro.mscatering.infrastructure.contract.consumer;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslJsonRootValue;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.mcalvaro.mscatering.infrastructure.contract.consumer.client.SubscriptionClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.http.HttpResponse;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba de Contrato — Lado del CONSUMIDOR.
 * <p>
 * Representa al consumidor "NutricenterPortalConsumer" (Portal Web / App Móvil
 * de Nur-tricenter que gestiona suscripciones). Esta clase define las
 * expectativas del consumidor sobre la API del proveedor
 * "ms-catering-subscription" (BC3).
 * <p>
 * Pact levanta un servidor mock temporal y, al ejecutar el cliente contra él,
 * genera automáticamente el archivo de contrato JSON en:
 * {@code target/pacts/NutricenterPortalConsumer-ms-catering-subscription.json}
 * <p>
 * Interacciones pactadas:
 * <ol>
 *   <li>POST /api/subscriptions — Crea una nueva suscripción de catering.
 *       Espera HTTP 201 con el UUID de la suscripción creada.</li>
 *   <li>GET /api/subscriptions/{id} — Consulta el estado de una suscripción.
 *       Espera HTTP 200 con el DTO de detalles de la suscripción.</li>
 * </ol>
 */
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "ms-catering-subscription", pactVersion = PactSpecVersion.V3)
class SubscriptionConsumerPactTest {

    // ─────────────────────────────────────────────────────────────────────────
    // Interacción 1: POST /api/subscriptions — Crear suscripción
    // ─────────────────────────────────────────────────────────────────────────

    @Pact(consumer = "NutricenterPortalConsumer", provider = "ms-catering-subscription")
    RequestResponsePact createSubscriptionPact(PactDslWithProvider builder) {
        return builder
                .given("a patient with ID 550e8400-e29b-41d4-a716-446655440000 exists and is eligible")
                .uponReceiving("una solicitud para crear una nueva suscripcion de catering de 15 dias")
                    .method("POST")
                    .path("/api/subscriptions")
                    .headers(Map.of("Content-Type", "application/json"))
                    .body(new PactDslJsonBody()
                            .uuid("patientId", "550e8400-e29b-41d4-a716-446655440000")
                            .uuid("dietPlanId", "660e8400-e29b-41d4-a716-446655440001")
                            .stringValue("startDate", "2026-10-01")
                            .stringValue("endDate", "2026-10-15")
                            .stringValue("serviceType", "LUNCH")
                            .decimalType("totalPrice", 250.00)
                            .stringValue("acceptedConditions",
                                    "Acepto los terminos y condiciones del servicio de catering.")
                            .stringValue("prefStreet", "Av. Siempre Viva")
                            .stringValue("prefNumber", "742")
                            .stringValue("prefCity", "Lima")
                            .stringValue("prefReference", "Frente al parque")
                            .numberType("prefLatitude", -12.046374)
                            .numberType("prefLongitude", -77.042793)
                            .stringValue("prefPhone", "+51 999 888 777")
                            .stringValue("prefTimeStart", "12:00:00")
                            .stringValue("prefTimeEnd", "14:00:00")
                            .stringValue("prefSpecialInstructions", "Sin picante"))
                .willRespondWith()
                    .status(201)
                    .headers(Map.of("Content-Type", "application/json"))
                    .body(PactDslJsonRootValue.stringMatcher(
                            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}",
                            "550e8400-e29b-41d4-a716-446655440099"))
                .toPact();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Interacción 2: GET /api/subscriptions/{id} — Consultar suscripción
    // ─────────────────────────────────────────────────────────────────────────

    @Pact(consumer = "NutricenterPortalConsumer", provider = "ms-catering-subscription")
    RequestResponsePact getSubscriptionDetailsPact(PactDslWithProvider builder) {
        return builder
                .given("a subscription with ID 770e8400-e29b-41d4-a716-446655440002 exists")
                .uponReceiving("una solicitud para consultar los detalles de una suscripcion existente")
                    .method("GET")
                    .path("/api/subscriptions/770e8400-e29b-41d4-a716-446655440002")
                    .headers(Map.of("Accept", "application/json"))
                .willRespondWith()
                    .status(200)
                    .headers(Map.of("Content-Type", "application/json"))
                    .body(new PactDslJsonBody()
                            .uuid("id")
                            .uuid("patientId")
                            .uuid("dietPlanId")
                            .stringType("contractCode")
                            .stringType("status")
                            .stringType("startDate")
                            .stringType("endDate")
                            .stringType("serviceType")
                            .decimalType("totalPrice")
                            .minArrayLike("deliveryDays", 1,
                                    new PactDslJsonBody()
                                            .uuid("id")
                                            .stringType("date")
                                            .stringType("status"))
                            .minArrayLike("evaluations", 1,
                                    new PactDslJsonBody()
                                            .uuid("id")
                                            .stringType("scheduledDate")
                                            .stringType("status")))
                .toPact();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 1: Verifica la interacción POST contra el mock server
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @PactTestFor(pactMethod = "createSubscriptionPact")
    @DisplayName("Consumidor: POST /api/subscriptions debe retornar 201 con UUID al crear suscripcion")
    void shouldCreateSubscriptionAndReturn201(MockServer mockServer) throws Exception {
        // Arrange: payload de creación conforme al contrato pactado
        String requestBody = """
                {
                  "patientId": "550e8400-e29b-41d4-a716-446655440000",
                  "dietPlanId": "660e8400-e29b-41d4-a716-446655440001",
                  "startDate": "2026-10-01",
                  "endDate": "2026-10-15",
                  "serviceType": "LUNCH",
                  "totalPrice": 250.00,
                  "acceptedConditions": "Acepto los terminos y condiciones del servicio de catering.",
                  "prefStreet": "Av. Siempre Viva",
                  "prefNumber": "742",
                  "prefCity": "Lima",
                  "prefReference": "Frente al parque",
                  "prefLatitude": -12.046374,
                  "prefLongitude": -77.042793,
                  "prefPhone": "+51 999 888 777",
                  "prefTimeStart": "12:00:00",
                  "prefTimeEnd": "14:00:00",
                  "prefSpecialInstructions": "Sin picante"
                }
                """;

        SubscriptionClient client = new SubscriptionClient(mockServer.getUrl());

        // Act
        HttpResponse<String> response = client.createSubscription(requestBody);

        // Assert: el consumidor recibe 201 y un cuerpo que contiene el UUID
        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.body()).isNotBlank();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 2: Verifica la interacción GET contra el mock server
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @PactTestFor(pactMethod = "getSubscriptionDetailsPact")
    @DisplayName("Consumidor: GET /api/subscriptions/{id} debe retornar 200 con los detalles de la suscripcion")
    void shouldGetSubscriptionDetailsAndReturn200(MockServer mockServer) throws Exception {
        String subscriptionId = "770e8400-e29b-41d4-a716-446655440002";

        SubscriptionClient client = new SubscriptionClient(mockServer.getUrl());

        // Act
        HttpResponse<String> response = client.getSubscriptionDetails(subscriptionId);

        // Assert: el consumidor recibe 200 y un cuerpo con los campos esperados
        assertThat(response.statusCode()).isEqualTo(200);
        var body = client.parseBody(response.body());
        assertThat(body.has("id")).isTrue();
        assertThat(body.has("status")).isTrue();
        assertThat(body.has("contractCode")).isTrue();
        assertThat(body.has("deliveryDays")).isTrue();
        assertThat(body.has("evaluations")).isTrue();
    }
}
