package com.mcalvaro.mscatering.application.patient.address.synchronization;

import an.awesome.pipelinr.Command;

/**
 * Handles local BC1 address projection commands dispatched through Pipelinr.
 */
public class SynchronizePatientAddressCommandHandler
        implements Command.Handler<SynchronizePatientAddressCommand, Void> {

    private final PatientAddressSynchronizationStore synchronizationStore;

    public SynchronizePatientAddressCommandHandler(PatientAddressSynchronizationStore synchronizationStore) {
        this.synchronizationStore = synchronizationStore;
    }

    @Override
    public Void handle(SynchronizePatientAddressCommand command) {
        synchronizationStore.synchronize(command);
        return null;
    }
}
