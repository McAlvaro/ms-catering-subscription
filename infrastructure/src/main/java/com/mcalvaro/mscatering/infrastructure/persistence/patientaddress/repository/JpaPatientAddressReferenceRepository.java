package com.mcalvaro.mscatering.infrastructure.persistence.patientaddress.repository;

import com.mcalvaro.mscatering.domain.patient.IPatientAddressReferenceRepository;
import com.mcalvaro.mscatering.domain.patient.PatientAddressReference;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaPatientAddressReferenceRepository implements IPatientAddressReferenceRepository {

    private final SpringDataPatientAddressReferenceRepository repository;

    public JpaPatientAddressReferenceRepository(SpringDataPatientAddressReferenceRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<PatientAddressReference> findByAddressId(UUID addressId) {
        return repository.findById(addressId).map(entity -> new PatientAddressReference(entity.getAddressId(),
                entity.getPatientId(), entity.getAlias(), entity.getStreet(), entity.getReference(), entity.getCity(),
                entity.getLatitude(), entity.getLongitude()));
    }
}
