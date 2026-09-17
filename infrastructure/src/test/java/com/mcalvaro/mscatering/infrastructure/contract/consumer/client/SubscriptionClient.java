package com.mcalvaro.mscatering.infrastructure.contract.consumer.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Cliente HTTP que modela cómo el consumidor ("NutricenterPortalConsumer")
 * interactúa con el microservicio ms-catering-subscription (BC3).
 * <p>
 * Este cliente es la pieza central del Consumer-Driven Contract Testing:
 * la prueba del consumidor lo ejecuta contra un Mock Server de Pact, que
 * captura las interacciones y genera el contrato JSON.
 * El mismo contrato es luego verificado por el Provider Test.
 */
public class SubscriptionClient {

    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public SubscriptionClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule());
    }

    /**
     * Solicita la creación de una nueva suscripción de catering.
     * Corresponde a: POST /api/subscriptions
     *
     * @param requestBody JSON del comando CreateSubscriptionCommand
     * @return respuesta HTTP (status code + body con UUID)
     */
    public HttpResponse<String> createSubscription(String requestBody) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/subscriptions"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Consulta los detalles y el estado de una suscripción existente.
     * Corresponde a: GET /api/subscriptions/{id}
     *
     * @param subscriptionId UUID de la suscripción a consultar
     * @return respuesta HTTP (status code + body con SubscriptionDetailsDto)
     */
    public HttpResponse<String> getSubscriptionDetails(String subscriptionId) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/subscriptions/" + subscriptionId))
                .header("Accept", "application/json")
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Parsea el cuerpo de la respuesta como un árbol JSON.
     */
    public JsonNode parseBody(String body) throws IOException {
        return objectMapper.readTree(body);
    }
}
