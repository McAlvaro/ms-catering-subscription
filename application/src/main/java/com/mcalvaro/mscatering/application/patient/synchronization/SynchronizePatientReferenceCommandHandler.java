package com.mcalvaro.mscatering.application.patient.synchronization;

import an.awesome.pipelinr.Command;

/** Handles the PII-free local command through the persistence port. */
public class SynchronizePatientReferenceCommandHandler
        implements Command.Handler<SynchronizePatientReferenceCommand, Void> {

    private final PatientReferenceSynchronizationStore synchronizationStore;

    public SynchronizePatientReferenceCommandHandler(PatientReferenceSynchronizationStore synchronizationStore) {
        this.synchronizationStore = synchronizationStore;
    }

    @Override
    public Void handle(SynchronizePatientReferenceCommand command) {
        synchronizationStore.synchronize(command);
        return null;
    }
}
