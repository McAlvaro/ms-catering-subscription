package com.mcalvaro.mscatering.application.outbox;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class OutboxMessageTest {

    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final Instant CREATED = Instant.parse("2026-10-28T10:15:30.123456789Z");

    @Test
    void pendingFactoryCreatesAnUnlockedUnprocessedMessageAvailableWhenCreated() {
        OutboxMessage message = OutboxMessage.pending(ID, "bc3.SubscriptionCreated", "{\"id\":\"1\"}", CREATED);

        assertThat(message.id()).isEqualTo(ID);
        assertThat(message.type()).isEqualTo("bc3.SubscriptionCreated");
        assertThat(message.content()).isEqualTo("{\"id\":\"1\"}");
        assertThat(message.created()).isEqualTo(CREATED.truncatedTo(ChronoUnit.MICROS));
        assertThat(message.availableAt()).isEqualTo(CREATED.truncatedTo(ChronoUnit.MICROS));
        assertThat(message.processed()).isFalse();
        assertThat(message.processedOn()).isNull();
        assertThat(message.attemptCount()).isZero();
        assertThat(message.lastError()).isNull();
        assertThat(message.lockedUntil()).isNull();
        assertThat(message.lockedBy()).isNull();
    }

    @Test
    void normalizesEveryTemporalFieldToMicroseconds() {
        OutboxMessage processed = message(CREATED, true, CREATED.plusNanos(999), CREATED.plusNanos(1), null);
        OutboxMessage locked = message(CREATED, false, null, CREATED.plusNanos(1), CREATED.plusNanos(777));

        assertThat(processed.created()).isEqualTo(CREATED.truncatedTo(ChronoUnit.MICROS));
        assertThat(processed.processedOn()).isEqualTo(CREATED.plusNanos(999).truncatedTo(ChronoUnit.MICROS));
        assertThat(processed.availableAt()).isEqualTo(CREATED.truncatedTo(ChronoUnit.MICROS));
        assertThat(locked.lockedUntil()).isEqualTo(CREATED.plusNanos(777).truncatedTo(ChronoUnit.MICROS));
    }

    @Test
    void rejectsRequiredAndBlankFields() {
        assertThatNullPointerException().isThrownBy(() -> new OutboxMessage(null, "type", "{}", CREATED, false,
                null, 0, null, CREATED, null, null, null, null, null));
        assertThatNullPointerException().isThrownBy(() -> new OutboxMessage(ID, "type", "{}", null, false,
                null, 0, null, CREATED, null, null, null, null, null));
        assertThatNullPointerException().isThrownBy(() -> new OutboxMessage(ID, "type", "{}", CREATED, false,
                null, 0, null, null, null, null, null, null, null));
        assertThatIllegalArgumentException().isThrownBy(() -> valid(" ", "{}", 0, false, null, null, null, null,
                null, null, null, CREATED));
        assertThatIllegalArgumentException().isThrownBy(() -> valid("type", "\t", 0, false, null, null, null,
                null, null, null, null, CREATED));
    }

    @Test
    void rejectsInvalidProcessingAndLockState() {
        assertThatIllegalArgumentException().isThrownBy(() -> valid("type", "{}", -1, false, null, null, null,
                null, null, null, null, CREATED));
        assertThatIllegalArgumentException().isThrownBy(() -> valid("type", "{}", 0, true, null, null, null,
                null, null, null, null, CREATED));
        assertThatIllegalArgumentException().isThrownBy(() -> valid("type", "{}", 0, false, CREATED, null, null,
                null, null, null, null, CREATED));
        assertThatIllegalArgumentException().isThrownBy(() -> valid("type", "{}", 0, false, null,
                CREATED.plusSeconds(1), null, null, null, null, null, CREATED));
        assertThatIllegalArgumentException().isThrownBy(() -> valid("type", "{}", 0, false, null, null, "worker",
                null, null, null, null, CREATED));
        assertThatIllegalArgumentException().isThrownBy(() -> valid("type", "{}", 0, true, CREATED,
                CREATED.plusSeconds(1), "worker", null, null, null, null, CREATED));
    }

    @Test
    void rejectsTemporalOrderAndBlankOptionalStrings() {
        assertThatIllegalArgumentException().isThrownBy(() -> valid("type", "{}", 0, false, null, null, null,
                null, null, null, null, CREATED.minus(1, ChronoUnit.MICROS)));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> valid("type", "{}", 0, true, CREATED.minus(1, ChronoUnit.MICROS),
                        null, null, null, null, null, null, CREATED));
        assertThatIllegalArgumentException().isThrownBy(() -> valid("type", "{}", 0, false, null, null, null, " ",
                null, null, null, CREATED));
        assertThatIllegalArgumentException().isThrownBy(() -> valid("type", "{}", 0, false, null, null, null, null,
                " ", null, null, CREATED));
        assertThatIllegalArgumentException().isThrownBy(() -> valid("type", "{}", 0, false, null, null, null, null,
                null, " ", null, CREATED));
        assertThatIllegalArgumentException().isThrownBy(() -> valid("type", "{}", 0, false, null, null, null, null,
                null, null, " ", CREATED));
    }

    private OutboxMessage message(Instant created, boolean processed, Instant processedOn, Instant availableAt,
            Instant lockedUntil) {
        return new OutboxMessage(ID, "type", "{}", created, processed, processedOn, 2, "failure", availableAt,
                lockedUntil, lockedUntil == null ? null : "worker", "correlation", "trace", "span");
    }

    private OutboxMessage valid(String type, String content, int attempts, boolean processed, Instant processedOn,
            Instant lockedUntil, String lockedBy, String lastError, String correlationId, String traceId, String spanId,
            Instant availableAt) {
        return new OutboxMessage(ID, type, content, CREATED, processed, processedOn, attempts, lastError, availableAt,
                lockedUntil, lockedBy, correlationId, traceId, spanId);
    }
}
