package com.mcalvaro.mscatering.application.patient.synchronization;

/**
 * Port for atomically storing the minimal patient reference and its
 * inbound-event
 * receipt.
 */
public interface PatientReferenceSynchronizationStore {

    void synchronize(SynchronizePatientReferenceCommand command);
}
