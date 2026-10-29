package com.mcalvaro.mscatering.infrastructure.persistence.outbox.codec;

import com.mcalvaro.mscatering.domain.consolidatedcalendar.entity.ConsolidatedDeliverySnapshot;
import com.mcalvaro.mscatering.domain.consolidatedcalendar.entity.ConsolidatedRecipeSnapshot;
import com.mcalvaro.mscatering.domain.consolidatedcalendar.event.ConsolidatedCalendarClosed;
import com.mcalvaro.mscatering.domain.core.DomainEvent;
import com.mcalvaro.mscatering.domain.dietplan.PlanType;
import com.mcalvaro.mscatering.domain.subscription.event.SubscriptionCancelled;
import com.mcalvaro.mscatering.domain.subscription.event.SubscriptionCompleted;
import com.mcalvaro.mscatering.domain.subscription.event.SubscriptionCreated;
import com.mcalvaro.mscatering.domain.subscription.event.SubscriptionPaused;
import com.mcalvaro.mscatering.domain.subscription.event.SubscriptionReactivated;
import com.mcalvaro.mscatering.domain.subscription.vo.ContractCode;
import com.mcalvaro.mscatering.domain.subscription.vo.DeliveryAddress;
import com.mcalvaro.mscatering.domain.subscription.vo.PauseRange;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class JacksonDomainEventCodecTest {

    private final JacksonDomainEventCodec codec = new JacksonDomainEventCodec();

    @ParameterizedTest
    @MethodSource("registeredEvents")
    void roundTripsEveryRegisteredDomainEventExactly(String expectedType, DomainEvent event) {
        EncodedDomainEvent encoded = codec.encode(event);

        DomainEvent decoded = codec.decode(encoded.type(), encoded.content());

        assertThat(encoded.type()).isEqualTo(expectedType);
        assertThat(decoded).isExactlyInstanceOf(event.getClass()).isEqualTo(event);
        assertThat(decoded.eventId()).isEqualTo(event.eventId());
        assertThat(decoded.occurredAt()).isEqualTo(event.occurredAt());
    }

    @Test
    void preservesEveryNestedClosedCalendarDeliveryAddressRecipeAndListOrder() {
        ConsolidatedCalendarClosed event = closedCalendarEvent();

        ConsolidatedCalendarClosed decoded = (ConsolidatedCalendarClosed) codec.decode(
                codec.encode(event).type(), codec.encode(event).content());

        assertThat(decoded.correlationId()).isEqualTo("correlation-calendar");
        assertThat(decoded.deliveries()).containsExactlyElementsOf(event.deliveries());
        assertThat(decoded.deliveries().get(0).address()).isEqualTo(event.deliveries().get(0).address());
        assertThat(decoded.deliveries().get(0).recipes())
                .containsExactlyElementsOf(event.deliveries().get(0).recipes());
    }

    @Test
    void emitsOpaquePayloadWithoutClassMetadata() {
        EncodedDomainEvent encoded = codec.encode(subscriptionCreated());

        assertThat(encoded.content())
                .doesNotContain(SubscriptionCreated.class.getName())
                .doesNotContain("@class")
                .doesNotContain("@type");
    }

    @Test
    void rejectsNullAndUnsupportedEvents() {
        assertThatIllegalArgumentException().isThrownBy(() -> codec.encode(null))
                .withMessage("Domain event must not be null");
        assertThatIllegalArgumentException().isThrownBy(() -> codec.encode(
                new UnsupportedEvent(UUID.fromString("00000000-0000-0000-0000-000000000099"), FIXED_INSTANT)))
                .withMessage("Unsupported domain event class: %s", UnsupportedEvent.class.getName());
    }

    @Test
    void rejectsBlankInputsAndUnknownTypesBeforeDeserialization() {
        assertThatIllegalArgumentException().isThrownBy(() -> codec.decode(null, "{}"))
                .withMessage("Domain event type must not be blank");
        assertThatIllegalArgumentException().isThrownBy(() -> codec.decode(" ", "{}"))
                .withMessage("Domain event type must not be blank");
        assertThatIllegalArgumentException().isThrownBy(() -> codec.decode("subscription.created", " "))
                .withMessage("Domain event content must not be blank");
        assertThatIllegalArgumentException().isThrownBy(() -> codec.decode("unregistered.event", "{not-json"))
                .withMessage("Unknown domain event type: unregistered.event");
    }

    @Test
    void rejectsBlankEncodedEventFields() {
        assertThatIllegalArgumentException().isThrownBy(() -> new EncodedDomainEvent(" ", "{}"))
                .withMessage("Domain event type must not be blank");
        assertThatIllegalArgumentException().isThrownBy(() -> new EncodedDomainEvent("subscription.created", null))
                .withMessage("Domain event content must not be blank");
    }

    @Test
    void rejectsMalformedJsonDuplicateKeysAndTrailingTokens() {
        assertMalformed("{not-json");
        String paused = codec.encode(subscriptionPaused()).content();
        assertMalformed(paused.replace("\"eventId\":\"00000000-0000-0000-0000-000000000007\",",
                "\"eventId\":\"00000000-0000-0000-0000-000000000007\","
                        + "\"eventId\":\"00000000-0000-0000-0000-000000000007\","));
        assertMalformed(codec.encode(subscriptionPaused()).content() + " {} ");
    }

    @Test
    void rejectsValidJsonThatIsIncompatibleWithTheDeclaredEventType() {
        EncodedDomainEvent paused = codec.encode(subscriptionPaused());
        String created = codec.encode(subscriptionCreated()).content();

        assertIncompatible("subscription.created", paused.content());
        assertIncompatible("subscription.created", created.replace("}", ",\"unexpected\":true}"));
        assertIncompatible("subscription.created", "{\"eventId\":\"00000000-0000-0000-0000-000000000001\"}");
        assertIncompatible("subscription.created", created.replace("\"planType\":\"BASE\"", "\"planType\":1"));
        assertIncompatible("subscription.created", created.replace("\"durationDays\":15", "\"durationDays\":\"15\""));
        assertIncompatible("subscription.created", created.replace("\"durationDays\":15", "\"durationDays\":null"));
        assertIncompatible("subscription.created", created.replace("\"durationDays\":15", "\"durationDays\":15.5"));
    }

    private void assertMalformed(String content) {
        assertThatIllegalArgumentException().isThrownBy(() -> codec.decode("subscription.paused", content))
                .withMessage("Malformed JSON for domain event type 'subscription.paused'")
                .havingCause();
    }

    private void assertIncompatible(String type, String content) {
        assertThatIllegalArgumentException().isThrownBy(() -> codec.decode(type, content))
                .withMessage("JSON is incompatible with domain event type '%s'", type)
                .havingCause();
    }

    private static Stream<Arguments> registeredEvents() {
        return Stream.of(
                Arguments.of("subscription.created", subscriptionCreated()),
                Arguments.of("subscription.paused", subscriptionPaused()),
                Arguments.of("subscription.reactivated", subscriptionReactivated()),
                Arguments.of("subscription.cancelled", subscriptionCancelled()),
                Arguments.of("subscription.completed", subscriptionCompleted()),
                Arguments.of("consolidated-calendar.closed", closedCalendarEvent()));
    }

    private static SubscriptionCreated subscriptionCreated() {
        return new SubscriptionCreated(UUID.fromString("00000000-0000-0000-0000-000000000001"), FIXED_INSTANT,
                UUID.fromString("00000000-0000-0000-0000-000000000002"),
                UUID.fromString("00000000-0000-0000-0000-000000000003"), new ContractCode("NTC-2026-0001"),
                UUID.fromString("00000000-0000-0000-0000-000000000004"), PlanType.BASE,
                LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 24), 15,
                List.of(UUID.fromString("00000000-0000-0000-0000-000000000005"),
                        UUID.fromString("00000000-0000-0000-0000-000000000006")),
                new BigDecimal("249.90"), "correlation-created");
    }

    private static SubscriptionPaused subscriptionPaused() {
        return new SubscriptionPaused(UUID.fromString("00000000-0000-0000-0000-000000000007"), FIXED_INSTANT,
                UUID.fromString("00000000-0000-0000-0000-000000000002"),
                UUID.fromString("00000000-0000-0000-0000-000000000003"),
                new PauseRange(LocalDate.of(2024, 2, 10), LocalDate.of(2024, 2, 12)), "Travel",
                LocalDate.of(2024, 2, 1), LocalDate.of(2026, 1, 27), "correlation-paused");
    }

    private static SubscriptionReactivated subscriptionReactivated() {
        return new SubscriptionReactivated(UUID.fromString("00000000-0000-0000-0000-000000000008"), FIXED_INSTANT,
                UUID.fromString("00000000-0000-0000-0000-000000000002"),
                UUID.fromString("00000000-0000-0000-0000-000000000003"), LocalDate.of(2026, 1, 12),
                LocalDate.of(2026, 1, 25), "correlation-reactivated");
    }

    private static SubscriptionCancelled subscriptionCancelled() {
        return new SubscriptionCancelled(UUID.fromString("00000000-0000-0000-0000-000000000009"), FIXED_INSTANT,
                UUID.fromString("00000000-0000-0000-0000-000000000002"),
                UUID.fromString("00000000-0000-0000-0000-000000000003"),
                UUID.fromString("00000000-0000-0000-0000-000000000004"), "Requested by patient",
                LocalDate.of(2026, 1, 13), 3, "correlation-cancelled");
    }

    private static SubscriptionCompleted subscriptionCompleted() {
        return new SubscriptionCompleted(UUID.fromString("00000000-0000-0000-0000-000000000010"), FIXED_INSTANT,
                UUID.fromString("00000000-0000-0000-0000-000000000002"),
                UUID.fromString("00000000-0000-0000-0000-000000000003"),
                UUID.fromString("00000000-0000-0000-0000-000000000004"), LocalDate.of(2026, 1, 24), 2,
                "correlation-completed");
    }

    private static ConsolidatedCalendarClosed closedCalendarEvent() {
        return new ConsolidatedCalendarClosed(UUID.fromString("00000000-0000-0000-0000-000000000011"), FIXED_INSTANT,
                UUID.fromString("00000000-0000-0000-0000-000000000012"), LocalDate.of(2026, 1, 10), 2,
                "correlation-calendar", List.of(delivery("013", "Patient One", "First Street"),
                        delivery("014", "Patient Two", "Second Street")));
    }

    private static ConsolidatedDeliverySnapshot delivery(String suffix, String patientName, String street) {
        return new ConsolidatedDeliverySnapshot(UUID.fromString("00000000-0000-0000-0000-000000000" + suffix),
                UUID.fromString("00000000-0000-0000-0000-000000000020"),
                UUID.fromString("00000000-0000-0000-0000-000000000021"), patientName,
                UUID.fromString("00000000-0000-0000-0000-000000000022"), 1,
                new DeliveryAddress(UUID.fromString("00000000-0000-0000-0000-000000000023"), "Home", street, "10",
                        "Madrid", "Reception", 40.4168, -3.7038, "600000000"),
                List.of(new ConsolidatedRecipeSnapshot(UUID.fromString("00000000-0000-0000-0000-000000000024"),
                        "Soup", LocalTime.of(12, 30), 250),
                        new ConsolidatedRecipeSnapshot(UUID.fromString("00000000-0000-0000-0000-000000000025"),
                                "Salad", LocalTime.of(13, 0), 180)));
    }

    private record UnsupportedEvent(UUID eventId, Instant occurredAt) implements DomainEvent {
    }

    private static final Instant FIXED_INSTANT = Instant.parse("2026-01-01T10:15:30.123456789Z");
}
