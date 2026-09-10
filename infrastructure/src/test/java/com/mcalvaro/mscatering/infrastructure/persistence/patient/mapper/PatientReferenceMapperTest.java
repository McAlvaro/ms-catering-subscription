package com.mcalvaro.mscatering.infrastructure.persistence.patient.mapper;

import com.mcalvaro.mscatering.domain.patient.PatientReference;
import com.mcalvaro.mscatering.infrastructure.persistence.patient.entity.PatientReferenceJpaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PatientReferenceMapperTest {

    private final PatientReferenceMapper mapper = new PatientReferenceMapper();

    @Test
    @DisplayName("Should map every patient reference field to its JPA representation")
    void shouldMapPatientReferenceToJpaWhenDomainReferenceIsProvided() {
        // Arrange
        PatientReference reference = buildPatientReference();

        // Act
        PatientReferenceJpaEntity result = mapper.toJpaEntity(reference);

        // Assert
        assertThat(result.getPatientId()).isEqualTo(reference.patientId());
        assertThat(result.isActive()).isEqualTo(reference.isActive());
        assertThat(result.getUpdatedAt()).isEqualTo(reference.updatedAt());
    }

    @Test
    @DisplayName("Should map every patient reference field to its domain representation")
    void shouldMapPatientReferenceToDomainWhenJpaReferenceIsProvided() {
        // Arrange
        PatientReferenceJpaEntity reference = buildPatientReferenceJpaEntity();

        // Act
        PatientReference result = mapper.toDomain(reference);

        // Assert
        assertThat(result.patientId()).isEqualTo(reference.getPatientId());
        assertThat(result.isActive()).isEqualTo(reference.isActive());
        assertThat(result.updatedAt()).isEqualTo(reference.getUpdatedAt());
    }

    @Test
    @DisplayName("Should return null when the patient reference source is null")
    void shouldReturnNullWhenPatientReferenceSourceIsNull() {
        // Arrange
        PatientReference domainReference = null;
        PatientReferenceJpaEntity jpaReference = null;

        // Act
        PatientReferenceJpaEntity jpaResult = mapper.toJpaEntity(domainReference);
        PatientReference domainResult = mapper.toDomain(jpaReference);

        // Assert
        assertThat(jpaResult).isNull();
        assertThat(domainResult).isNull();
    }

    private PatientReference buildPatientReference() {
        return new PatientReference(UUID.fromString("1b896fcb-dfac-4798-ae62-f0731b9a051b"), false,
                Instant.parse("2026-03-10T09:15:30Z"));
    }

    private PatientReferenceJpaEntity buildPatientReferenceJpaEntity() {
        PatientReferenceJpaEntity entity = new PatientReferenceJpaEntity();
        entity.setPatientId(UUID.fromString("4af6eebe-981b-4160-aede-50fa764a41cb"));
        entity.setActive(true);
        entity.setUpdatedAt(Instant.parse("2026-04-11T10:20:30Z"));
        return entity;
    }
}
