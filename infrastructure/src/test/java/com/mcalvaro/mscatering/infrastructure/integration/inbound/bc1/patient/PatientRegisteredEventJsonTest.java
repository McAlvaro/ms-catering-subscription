package com.mcalvaro.mscatering.infrastructure.integration.inbound.bc1.patient;

import an.awesome.pipelinr.Pipeline;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class PatientRegisteredEventJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Mock
    private Pipeline pipeline;

    @Test
    void readsTheExternalDataIdPacienteAndIgnoresAdditionalPii() throws Exception {
        UUID patientId = UUID.randomUUID();

        PatientRegisteredEvent event = objectMapper.readValue("""
                {
                  "eventId": "550e8400-e29b-41d4-a716-446655440000",
                  "eventName": "bc1.PacienteRegistrado",
                  "eventVersion": "1.0",
                  "occurredOn": "2026-10-27T12:00:00Z",
                  "source": "MsPatientsConsulCore",
                  "correlationId": "corr-123",
                  "data": {
                    "idPaciente": "%s",
                    "nombreCompleto": "María Pérez",
                    "fechaNacimiento": "1990-02-03",
                    "sexo": "FEMENINO",
                    "telefono": "+51 999 111 222",
                    "email": "maria.perez@example.com",
                    "idNutricionista": "770e8400-e29b-41d4-a716-446655440002",
                    "fechaRegistro": "2026-10-27"
                  }
                }
                """.formatted(patientId), PatientRegisteredEvent.class);

        assertThat(event.data()).isEqualTo(new PatientRegisteredData(patientId));
    }

    @Test
    void readsMinimalData() throws Exception {
        PatientRegisteredEvent event = objectMapper.readValue("""
                {"eventId":"550e8400-e29b-41d4-a716-446655440000","eventName":"bc1.PacienteRegistrado",
                 "eventVersion":"1.0","occurredOn":"2026-10-27T12:00:00Z","source":"MsPatientsConsulCore",
                 "correlationId":"corr-123","data":{"idPaciente":"660e8400-e29b-41d4-a716-446655440001"}}
                """, PatientRegisteredEvent.class);

        assertThat(event.data().patientId()).isEqualTo(UUID.fromString("660e8400-e29b-41d4-a716-446655440001"));
    }

    @Test
    void rejectsMalformedPatientUuidAndMissingConsumedDataBeforeDispatch() throws Exception {
        assertThatThrownBy(() -> objectMapper.readValue("""
                {"data":{"idPaciente":"not-a-uuid"}}
                """, PatientRegisteredEvent.class))
                .isInstanceOf(JsonProcessingException.class);

        PatientRegistrationInboundAdapter adapter = new PatientRegistrationInboundAdapter(pipeline);
        assertThatThrownBy(() -> adapter.receive(objectMapper.readValue(validEnvelopeWithoutData(),
                PatientRegisteredEvent.class)))
                .isInstanceOf(PatientRegistrationContractException.class)
                .extracting("code")
                .isEqualTo("PROJ-001");
        assertThatThrownBy(() -> adapter.receive(objectMapper.readValue(validEnvelopeWithData("{}"),
                PatientRegisteredEvent.class)))
                .isInstanceOf(PatientRegistrationContractException.class)
                .extracting("code")
                .isEqualTo("PROJ-001");
    }

    @Test
    void doesNotSupportLegacyPayloadOrPatientIdAliases() throws Exception {
        PatientRegistrationInboundAdapter adapter = new PatientRegistrationInboundAdapter(pipeline);

        assertThatThrownBy(() -> objectMapper.readValue("""
                {
                  "eventId":"550e8400-e29b-41d4-a716-446655440000",
                  "eventName":"bc1.PacienteRegistrado",
                  "eventVersion":"1.0",
                  "occurredOn":"2026-10-27T12:00:00Z",
                  "source":"MsPatientsConsulCore",
                  "correlationId":"corr-123",
                  "data":{"idPaciente":"660e8400-e29b-41d4-a716-446655440001"},
                  "payload":{"patientId":"660e8400-e29b-41d4-a716-446655440001"}
                }
                """, PatientRegisteredEvent.class))
                .isInstanceOf(JsonProcessingException.class);

        PatientRegisteredEvent legacyPatientId = objectMapper.readValue(validEnvelopeWithData("""
                {"patientId":"660e8400-e29b-41d4-a716-446655440001"}
                """), PatientRegisteredEvent.class);
        assertThat(legacyPatientId.data().patientId()).isNull();
        assertThatThrownBy(() -> adapter.receive(legacyPatientId))
                .isInstanceOf(PatientRegistrationContractException.class)
                .extracting("code")
                .isEqualTo("PROJ-001");
    }

    private String validEnvelopeWithoutData() {
        return """
                {"eventId":"550e8400-e29b-41d4-a716-446655440000","eventName":"bc1.PacienteRegistrado",
                 "eventVersion":"1.0","occurredOn":"2026-10-27T12:00:00Z","source":"MsPatientsConsulCore",
                 "correlationId":"corr-123"}
                """;
    }

    private String validEnvelopeWithData(String data) {
        return validEnvelopeWithoutData().replace("}", ",\"data\":" + data + "}");
    }
}
