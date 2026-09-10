package com.mcalvaro.mscatering.infrastructure.persistence.consolidatedcalendar.mapper;

import com.mcalvaro.mscatering.domain.consolidatedcalendar.ConsolidatedCalendar;
import com.mcalvaro.mscatering.domain.consolidatedcalendar.entity.ConsolidatedLine;
import com.mcalvaro.mscatering.domain.consolidatedcalendar.enums.ConsolidateStatus;
import com.mcalvaro.mscatering.domain.subscription.enums.ServiceType;
import com.mcalvaro.mscatering.domain.subscription.vo.DeliveryAddress;
import com.mcalvaro.mscatering.domain.subscription.vo.TimeWindow;
import com.mcalvaro.mscatering.infrastructure.persistence.consolidatedcalendar.entity.ConsolidatedCalendarJpaEntity;
import com.mcalvaro.mscatering.infrastructure.persistence.consolidatedcalendar.entity.ConsolidatedLineJpaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ConsolidatedCalendarMapperTest {

    private final ConsolidatedCalendarMapper mapper = new ConsolidatedCalendarMapper();

    @Test
    @DisplayName("Should map a closed calendar and its delivery line to JPA")
    void shouldMapClosedCalendarToJpaWhenDomainCalendarContainsLines() {
        // Arrange
        ConsolidatedCalendar calendar = buildClosedCalendar();

        // Act
        ConsolidatedCalendarJpaEntity result = mapper.toJpaEntity(calendar);

        // Assert
        assertThat(result.getId()).isEqualTo(calendar.getId());
        assertThat(result.getDate()).isEqualTo(calendar.getDate());
        assertThat(result.getStatus()).isEqualTo(ConsolidateStatus.CLOSED.name());
        assertThat(result.getTotalDeliveries()).isEqualTo(calendar.getTotalDeliveries());
        assertThat(result.getClosedAt()).isEqualTo(calendar.getClosedAt());
        assertThat(result.getClosedBy()).isEqualTo(calendar.getClosedBy());
        assertThat(result.getLines()).singleElement().satisfies(line -> {
            assertThat(line.getConsolidatedCalendar()).isSameAs(result);
            assertThat(line.getId()).isEqualTo(calendar.getLines().get(0).getId());
            assertThat(line.getSubscriptionId()).isEqualTo(calendar.getLines().get(0).getSubscriptionId());
            assertThat(line.getPatientId()).isEqualTo(calendar.getLines().get(0).getPatientId());
            assertThat(line.getDietPlanId()).isEqualTo(calendar.getLines().get(0).getDietPlanId());
            assertThat(line.getServiceType()).isEqualTo(calendar.getLines().get(0).getServiceType().name());
            assertThat(line.getAddressStreet()).isEqualTo("Main Street");
            assertThat(line.getAddressNumber()).isEqualTo("42");
            assertThat(line.getAddressCity()).isEqualTo("Madrid");
            assertThat(line.getAddressReference()).isEqualTo("Blue door");
            assertThat(line.getAddressLatitude()).isEqualTo(40.4168);
            assertThat(line.getAddressLongitude()).isEqualTo(-3.7038);
            assertThat(line.getAddressPhone()).isEqualTo("+34123456789");
            assertThat(line.getTimeStart()).isEqualTo("09:00");
            assertThat(line.getTimeEnd()).isEqualTo("11:00");
            assertThat(line.getInstructions()).isEqualTo("Leave with reception");
        });
    }

    @Test
    @DisplayName("Should map a JPA calendar including nullable coordinates to domain")
    void shouldMapCalendarToDomainWhenJpaCalendarContainsNullableCoordinates() {
        // Arrange
        ConsolidatedCalendarJpaEntity calendar = buildConsolidatedCalendarJpaEntity();

        // Act
        ConsolidatedCalendar result = mapper.toDomain(calendar);

        // Assert
        assertThat(result.getId()).isEqualTo(calendar.getId());
        assertThat(result.getDate()).isEqualTo(calendar.getDate());
        assertThat(result.getStatus()).isEqualTo(ConsolidateStatus.CLOSED);
        assertThat(result.getTotalDeliveries()).isEqualTo(1);
        assertThat(result.getClosedAt()).isEqualTo(calendar.getClosedAt());
        assertThat(result.getClosedBy()).isEqualTo(calendar.getClosedBy());
        assertThat(result.getLines()).singleElement().satisfies(line -> {
            assertThat(line.getId()).isEqualTo(calendar.getLines().get(0).getId());
            assertThat(line.getConsolidatedCalendarId()).isEqualTo(calendar.getId());
            assertThat(line.getSubscriptionId()).isEqualTo(calendar.getLines().get(0).getSubscriptionId());
            assertThat(line.getPatientId()).isEqualTo(calendar.getLines().get(0).getPatientId());
            assertThat(line.getDietPlanId()).isEqualTo(calendar.getLines().get(0).getDietPlanId());
            assertThat(line.getServiceType()).isEqualTo(ServiceType.values()[0]);
            assertThat(line.getAddress().latitude()).isZero();
            assertThat(line.getAddress().longitude()).isZero();
            assertThat(line.getAddress().street()).isEqualTo("Oak Avenue");
            assertThat(line.getAddress().number()).isNull();
            assertThat(line.getAddress().city()).isEqualTo("Seville");
            assertThat(line.getAddress().reference()).isNull();
            assertThat(line.getAddress().phone()).isEqualTo("+34987654321");
            assertThat(line.getTimeWindow().startTime()).isEqualTo(LocalTime.of(12, 0));
            assertThat(line.getTimeWindow().endTime()).isEqualTo(LocalTime.of(14, 0));
            assertThat(line.getInstructions()).isNull();
        });
    }

    @Test
    @DisplayName("Should return null when the consolidated calendar source is null")
    void shouldReturnNullWhenConsolidatedCalendarSourceIsNull() {
        // Arrange
        ConsolidatedCalendar domainCalendar = null;
        ConsolidatedCalendarJpaEntity jpaCalendar = null;

        // Act
        ConsolidatedCalendarJpaEntity jpaResult = mapper.toJpaEntity(domainCalendar);
        ConsolidatedCalendar domainResult = mapper.toDomain(jpaCalendar);

        // Assert
        assertThat(jpaResult).isNull();
        assertThat(domainResult).isNull();
    }

    private ConsolidatedCalendar buildClosedCalendar() {
        UUID calendarId = UUID.fromString("62ec74a8-a14a-4dfc-bc49-9d4c4a882169");
        ConsolidatedCalendar calendar = ConsolidatedCalendar.create(calendarId, LocalDate.of(2026, 5, 18));
        calendar.addLine(new ConsolidatedLine(UUID.fromString("a3d4481c-8293-48c4-bdfc-bfc2b5a9a73d"), calendarId,
                UUID.fromString("9a412c7d-19af-4a57-a9a8-0ea4517747bf"),
                UUID.fromString("980c15b9-6d3c-428f-893a-627208107d83"),
                UUID.fromString("671b9beb-e4e9-421e-a0e4-224ba1f0dd05"), ServiceType.values()[0], buildAddress(),
                new TimeWindow(LocalTime.of(9, 0), LocalTime.of(11, 0)), "Leave with reception"));
        calendar.close("dispatcher@example.com");
        return calendar;
    }

    private ConsolidatedCalendarJpaEntity buildConsolidatedCalendarJpaEntity() {
        ConsolidatedCalendarJpaEntity calendar = new ConsolidatedCalendarJpaEntity();
        calendar.setId(UUID.fromString("9c89ae74-2d0f-4b4a-bd3e-67a2132b3c08"));
        calendar.setDate(LocalDate.of(2026, 6, 3));
        calendar.setStatus(ConsolidateStatus.CLOSED.name());
        calendar.setTotalDeliveries(1);
        calendar.setClosedAt(Instant.parse("2026-06-02T17:25:00Z"));
        calendar.setClosedBy("operator@example.com");
        ConsolidatedLineJpaEntity line = new ConsolidatedLineJpaEntity();
        line.setId(UUID.fromString("5bd92248-84aa-4c23-a3ac-0912571331cf"));
        line.setSubscriptionId(UUID.fromString("f4943762-d722-41c3-8bdc-8974af149328"));
        line.setPatientId(UUID.fromString("2ee083a1-8bd2-4c75-82ca-c0c3718bfb12"));
        line.setDietPlanId(UUID.fromString("ebaf3703-8d88-4e04-9d4c-cad12fdc266f"));
        line.setServiceType(ServiceType.values()[0].name());
        line.setAddressStreet("Oak Avenue");
        line.setAddressNumber(null);
        line.setAddressCity("Seville");
        line.setAddressReference(null);
        line.setAddressLatitude(null);
        line.setAddressLongitude(null);
        line.setAddressPhone("+34987654321");
        line.setTimeStart("12:00");
        line.setTimeEnd("14:00");
        line.setInstructions(null);
        calendar.setLines(List.of(line));
        return calendar;
    }

    private DeliveryAddress buildAddress() {
        return new DeliveryAddress("Main Street", "42", "Madrid", "Blue door", 40.4168, -3.7038,
                "+34123456789");
    }
}
