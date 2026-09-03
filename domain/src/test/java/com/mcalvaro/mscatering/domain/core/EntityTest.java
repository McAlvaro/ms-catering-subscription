package com.mcalvaro.mscatering.domain.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EntityTest {

    @Test
    @DisplayName("Should reject a null identifier when creating an entity")
    void shouldRejectNullIdWhenCreatingEntity() {
        // Arrange
        UUID id = null;

        // Act & Assert
        assertThatThrownBy(() -> new TestEntity(id))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Entity id must not be null");
    }

    @Test
    @DisplayName("Should return the assigned identifier when created with an identifier")
    void shouldReturnIdWhenCreatedWithId() {
        // Arrange
        UUID id = UUID.randomUUID();
        TestEntity entity = new TestEntity(id);

        // Act
        UUID result = entity.getId();

        // Assert
        assertThat(result).isEqualTo(id);
    }

    @Test
    @DisplayName("Should accumulate domain events when events are added")
    void shouldAccumulateDomainEventsWhenEventsAreAdded() {
        // Arrange
        TestEntity entity = new TestEntity(UUID.randomUUID());
        DomainEvent firstEvent = new TestDomainEvent();
        DomainEvent secondEvent = new TestDomainEvent();

        // Act
        entity.record(firstEvent);
        entity.record(secondEvent);

        // Assert
        assertThat(entity.getDomainEvents()).containsExactly(firstEvent, secondEvent);
    }

    @Test
    @DisplayName("Should reject a null domain event when adding an event")
    void shouldRejectNullDomainEventWhenAddingEvent() {
        // Arrange
        TestEntity entity = new TestEntity(UUID.randomUUID());

        // Act & Assert
        assertThatThrownBy(() -> entity.record(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Domain event must not be null");
    }

    @Test
    @DisplayName("Should expose an unmodifiable event list when retrieving domain events")
    void shouldExposeUnmodifiableEventListWhenRetrievingDomainEvents() {
        // Arrange
        TestEntity entity = new TestEntity(UUID.randomUUID());
        DomainEvent event = new TestDomainEvent();
        entity.record(event);

        // Act & Assert
        assertThatThrownBy(() -> entity.getDomainEvents().add(new TestDomainEvent()))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(entity.getDomainEvents()).containsExactly(event);
    }

    @Test
    @DisplayName("Should remove all domain events when clearing events")
    void shouldRemoveAllDomainEventsWhenClearingEvents() {
        // Arrange
        TestEntity entity = new TestEntity(UUID.randomUUID());
        entity.record(new TestDomainEvent());

        // Act
        entity.clearDomainEvents();

        // Assert
        assertThat(entity.getDomainEvents()).isEmpty();
    }

    @Test
    @DisplayName("Should compare entities by identifier when checking equality")
    void shouldCompareEntitiesByIdWhenCheckingEquality() {
        // Arrange
        UUID id = UUID.randomUUID();
        TestEntity entity = new TestEntity(id);
        TestEntity entityWithSameId = new TestEntity(id);
        TestEntity entityWithDifferentId = new TestEntity(UUID.randomUUID());

        // Act & Assert
        assertThat(entity)
                .isEqualTo(entity)
                .isEqualTo(entityWithSameId)
                .isNotEqualTo(entityWithDifferentId)
                .isNotEqualTo("not an entity");
    }

    @Test
    @DisplayName("Should return the identifier hash code when calculating the hash code")
    void shouldReturnIdHashCodeWhenCalculatingHashCode() {
        // Arrange
        UUID id = UUID.randomUUID();
        TestEntity entity = new TestEntity(id);

        // Act
        int result = entity.hashCode();

        // Assert
        assertThat(result).isEqualTo(id.hashCode());
    }

    private static final class TestEntity extends Entity {

        private TestEntity(UUID id) {
            super(id);
        }

        private void record(DomainEvent event) {
            addDomainEvent(event);
        }
    }

    private record TestDomainEvent(UUID eventId, Instant occurredAt) implements DomainEvent {

        private TestDomainEvent() {
            this(UUID.randomUUID(), Instant.now());
        }
    }
}
