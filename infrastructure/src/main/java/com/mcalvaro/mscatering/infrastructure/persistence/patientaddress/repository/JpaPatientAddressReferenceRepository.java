package com.mcalvaro.mscatering.infrastructure.persistence.patientaddress.repository;

import com.mcalvaro.mscatering.domain.core.id.AddressId;
import com.mcalvaro.mscatering.domain.core.id.PatientId;
import com.mcalvaro.mscatering.domain.patient.IPatientAddressReferenceRepository;
import com.mcalvaro.mscatering.domain.patient.PatientAddressReference;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class JpaPatientAddressReferenceRepository implements IPatientAddressReferenceRepository {

    private final SpringDataPatientAddressReferenceRepository repository;

    public JpaPatientAddressReferenceRepository(SpringDataPatientAddressReferenceRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<PatientAddressReference> findByAddressId(AddressId addressId) {
        return repository.findById(addressId.value()).map(entity -> new PatientAddressReference(
                AddressId.of(entity.getAddressId()),
                PatientId.of(entity.getPatientId()),
                entity.getAlias(),
                entity.getStreet(),
                entity.getReference(),
                entity.getCity(),
                entity.getLatitude(),
                entity.getLongitude()));
    }
}
