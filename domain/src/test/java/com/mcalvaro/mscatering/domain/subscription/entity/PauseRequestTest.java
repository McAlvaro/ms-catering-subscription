package com.mcalvaro.mscatering.domain.subscription.entity;

import com.mcalvaro.mscatering.domain.subscription.vo.PauseRange;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PauseRequestTest {

    @Test
    @DisplayName("Should create an active pause request with its requested range and reason")
    void shouldCreateActivePauseRequestWhenValidDetailsAreProvided() {
        // Arrange
        UUID id = UUID.randomUUID();
        PauseRange range = pauseRange();
        Instant beforeCreation = Instant.now();

        // Act
        PauseRequest pauseRequest = new PauseRequest(id, range, "Vacation");
        Instant afterCreation = Instant.now();

        // Assert
        assertThat(pauseRequest.getId()).isEqualTo(id);
        assertThat(pauseRequest.getRange()).isEqualTo(range);
        assertThat(pauseRequest.getReason()).isEqualTo("Vacation");
        assertThat(pauseRequest.getCreatedAt()).isBetween(beforeCreation, afterCreation);
        assertThat(pauseRequest.getActualEndDate()).isNull();
        assertThat(pauseRequest.isActive()).isTrue();
    }

    @Test
    @DisplayName("Should end an active pause early and preserve its original request details")
    void shouldEarlyReactivateWhenReactivationDateIsProvided() {
        // Arrange
        PauseRange range = pauseRange();
        PauseRequest pauseRequest = new PauseRequest(UUID.randomUUID(), range, "Vacation");
        Instant createdAt = pauseRequest.getCreatedAt();
        LocalDate reactivationDate = range.startDate().plusDays(1);

        // Act
        pauseRequest.earlyReactivate(reactivationDate);

        // Assert
        assertThat(pauseRequest.isActive()).isFalse();
        assertThat(pauseRequest.getActualEndDate()).isEqualTo(reactivationDate);
        assertThat(pauseRequest.getRange()).isEqualTo(range);
        assertThat(pauseRequest.getReason()).isEqualTo("Vacation");
        assertThat(pauseRequest.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    @DisplayName("Should replace the actual end date when early reactivation is requested again")
    void shouldReplaceActualEndDateWhenEarlyReactivationIsRequestedAgain() {
        // Arrange
        PauseRequest pauseRequest = new PauseRequest(UUID.randomUUID(), pauseRange(), "Vacation");
        LocalDate firstReactivationDate = LocalDate.now().plusDays(4);
        LocalDate finalReactivationDate = firstReactivationDate.plusDays(1);
        pauseRequest.earlyReactivate(firstReactivationDate);

        // Act
        pauseRequest.earlyReactivate(finalReactivationDate);

        // Assert
        assertThat(pauseRequest.isActive()).isFalse();
        assertThat(pauseRequest.getActualEndDate()).isEqualTo(finalReactivationDate);
    }

    @Test
    @DisplayName("Should reject a null identifier through the inherited entity validation")
    void shouldThrowNullPointerExceptionWhenIdIsNull() {
        // Arrange
        PauseRange range = pauseRange();

        // Act & Assert
        assertThatThrownBy(() -> new PauseRequest(null, range, "Vacation"))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Entity id must not be null");
    }

    private PauseRange pauseRange() {
        LocalDate startDate = LocalDate.now().plusDays(3);
        return new PauseRange(startDate, startDate.plusDays(4));
    }
}
