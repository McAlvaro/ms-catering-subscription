package com.mcalvaro.mscatering.application.patient.address;

import com.mcalvaro.mscatering.domain.core.DomainException;
import com.mcalvaro.mscatering.domain.patient.IPatientAddressReferenceRepository;
import com.mcalvaro.mscatering.domain.patient.PatientAddressReference;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DeliveryAddressResolverTest {

    private final IPatientAddressReferenceRepository repository = mock(IPatientAddressReferenceRepository.class);
    private final DeliveryAddressResolver resolver = new DeliveryAddressResolver(repository);

    @Test
    void resolvesOnlyAnAddressOwnedByThePatient() {
        UUID patientId = UUID.randomUUID();
        UUID addressId = UUID.randomUUID();
        when(repository.findByAddressId(addressId)).thenReturn(Optional.of(new PatientAddressReference(addressId,
                patientId, "Home", "Main Street", "Opposite the park", "Cochabamba", -17.39, -66.15)));

        var address = resolver.resolve(patientId, addressId);

        assertThat(address.street()).isEqualTo("Main Street");
        assertThat(address.number()).isNull();
        assertThat(address.phone()).isNull();
        assertThat(address.city()).isEqualTo("Cochabamba");
    }

    @Test
    void rejectsMissingAndForeignAddressesWithTheSameDomainError() {
        UUID patientId = UUID.randomUUID();
        UUID addressId = UUID.randomUUID();
        when(repository.findByAddressId(addressId)).thenReturn(Optional.of(new PatientAddressReference(addressId,
                UUID.randomUUID(), "Work", "Street", "", "City", 0, 0)));

        assertThatThrownBy(() -> resolver.resolve(patientId, addressId))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("code", "ADR-001")
                .hasMessage(DeliveryAddressResolver.ADDRESS_NOT_AVAILABLE_MESSAGE);
    }
}
