package com.mcalvaro.mscatering.domain.core;

import java.time.Instant;
import java.util.UUID;

/**
 * Marker interface for all domain events.
 * <p>
 * Not sealed: permitted implementations span multiple sub-packages
 * (subscription.event, consolidatedcalendar.event) and this project
 * does not declare a named module (module-info.java), so Java's sealed
 * constraint — permitted subtypes must be in the same package — would
 * prevent the desired package organisation.
 * <p>
 * All implementing types are immutable {@code record}s that supply
 * their own {@code eventId} and {@code occurredAt} via a convenience
 * constructor that calls {@code UUID.randomUUID()} and {@code Instant.now()}.
 */
public interface DomainEvent {

    UUID eventId();

    Instant occurredAt();
}
