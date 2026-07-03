package com.mcalvaro.mscatering.application.abstractions;

/**
 * Port (abstraction) for transactional consistency.
 * <p>
 * The implementation in the infrastructure layer is responsible for:
 * <ul>
 * <li>Collecting domain events from all modified aggregates.</li>
 * <li>Publishing domain events (in-process or via a message broker).</li>
 * <li>Persisting all pending changes atomically.</li>
 * </ul>
 * <p>
 * This interface lives in the application layer (not in domain) because
 * the concept of "committing a transaction" is an application-level concern,
 * not a domain concern.
 */
public interface UnitOfWork {

    /**
     * Commits all pending changes and publishes accumulated domain events.
     */
    void commit();
}
