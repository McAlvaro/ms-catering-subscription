package com.mcalvaro.mscatering.infrastructure.api.patient;

import an.awesome.pipelinr.Pipeline;
import com.mcalvaro.mscatering.application.patient.SavePatientReferenceCommand;
import com.mcalvaro.mscatering.infrastructure.api.patient.dto.SavePatientRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final Pipeline pipeline;

    public PatientController(Pipeline pipeline) {
        this.pipeline = pipeline;
    }

    @PostMapping
    public ResponseEntity<Void> savePatient(@RequestBody SavePatientRequest request) {
        SavePatientReferenceCommand command = new SavePatientReferenceCommand(
                request.patientId(),
                request.active(),
                request.updatedAt());
        pipeline.send(command);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
