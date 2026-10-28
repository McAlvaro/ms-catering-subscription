package com.mcalvaro.mscatering.application.patient.address.synchronization;

/**
 * Atomic persistence port for an address projection and its inbound receipt.
 */
public interface PatientAddressSynchronizationStore {

    void synchronize(SynchronizePatientAddressCommand command);
}
