package com.mcalvaro.mscatering.infrastructure.integration.inbound.bc1.address;

import an.awesome.pipelinr.Pipeline;
import com.mcalvaro.mscatering.application.patient.address.synchronization.SynchronizePatientAddressCommand;
import com.mcalvaro.mscatering.domain.core.DomainException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PatientAddressRegistrationInboundAdapterTest {

    private final Pipeline pipeline = mock(Pipeline.class);
    private final PatientAddressRegistrationInboundAdapter adapter = new PatientAddressRegistrationInboundAdapter(
            pipeline);

    @Test
    void mapsOnlyConsumedAddressFieldsAndIgnoresAliasAndPrincipal() {
        PatientAddressRegisteredEvent event = event(-17.39, -66.15);
        when(pipeline.send(org.mockito.ArgumentMatchers.any(SynchronizePatientAddressCommand.class))).thenReturn(null);

        adapter.receive(event);

        var captor = ArgumentCaptor.forClass(SynchronizePatientAddressCommand.class);
        verify(pipeline).send(captor.capture());
        assertThat(captor.getValue().street()).isEqualTo("Main Street");
        assertThat(captor.getValue().addressId()).isEqualTo(event.data().addressId());
    }

    @Test
    void rejectsNonFiniteAndOutOfRangeCoordinates() {
        assertThatThrownBy(() -> adapter.receive(event(Double.NaN, -66.15)))
                .isInstanceOf(DomainException.class).hasFieldOrPropertyWithValue("code", "PROJ-001");
        assertThatThrownBy(() -> adapter.receive(event(-17.39, 181d)))
                .isInstanceOf(DomainException.class).hasFieldOrPropertyWithValue("code", "PROJ-001");
    }

    private PatientAddressRegisteredEvent event(double latitude, double longitude) {
        return new PatientAddressRegisteredEvent(UUID.randomUUID(), "bc1.DireccionClienteRegistrada", "1.0",
                Instant.parse("2026-10-27T12:00:00Z"), "MsPatientsConsulCore", "correlation",
                new PatientAddressRegisteredData(UUID.randomUUID(), UUID.randomUUID(), "Main Street", "Near park",
                        "Cochabamba", new PatientAddressRegisteredData.Geolocation(latitude, longitude)));
    }
}
