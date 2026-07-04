package com.mcalvaro.mscatering.application.patient;

import an.awesome.pipelinr.Command;
import com.mcalvaro.mscatering.domain.patient.IPatientReferenceRepository;
import com.mcalvaro.mscatering.domain.patient.PatientReference;

public class SavePatientReferenceCommandHandler implements Command.Handler<SavePatientReferenceCommand, Void> {

    private final IPatientReferenceRepository patientRepository;

    public SavePatientReferenceCommandHandler(
            IPatientReferenceRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    @Override
    public Void handle(SavePatientReferenceCommand command) {

        PatientReference patientReference = new PatientReference(
                command.patientId(),
                command.active(),
                command.updatedAt() != null ? command.updatedAt() : java.time.Instant.now());

        patientRepository.save(patientReference);

        return null;
    }
}
