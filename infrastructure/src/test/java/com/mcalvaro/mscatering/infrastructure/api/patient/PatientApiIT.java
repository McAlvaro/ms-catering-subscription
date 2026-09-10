package com.mcalvaro.mscatering.infrastructure.api.patient;

import com.mcalvaro.mscatering.domain.patient.IPatientReferenceRepository;
import com.mcalvaro.mscatering.domain.patient.PatientReference;
import com.mcalvaro.mscatering.infrastructure.BaseIntegrationTest;
import com.mcalvaro.mscatering.infrastructure.api.patient.dto.SavePatientRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PatientApiIT extends BaseIntegrationTest {

    @Autowired
    private IPatientReferenceRepository patientRepository;

    @Test
    @DisplayName("Should persist patient reference and return 201 Created when request is valid")
    void shouldPersistPatientReferenceAndReturnCreatedWhenRequestIsValid() throws Exception {
        // Arrange
        UUID patientId = UUID.randomUUID();
        Instant now = Instant.parse("2026-09-09T20:00:00Z");
        SavePatientRequest request = new SavePatientRequest(patientId, true, now);

        // Act
        mockMvc.perform(post("/api/patients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Assert
        Optional<PatientReference> persistedPatient = patientRepository.findById(patientId);
        assertThat(persistedPatient).isPresent();
        assertThat(persistedPatient.get().patientId()).isEqualTo(patientId);
        assertThat(persistedPatient.get().isActive()).isTrue();
    }
}
