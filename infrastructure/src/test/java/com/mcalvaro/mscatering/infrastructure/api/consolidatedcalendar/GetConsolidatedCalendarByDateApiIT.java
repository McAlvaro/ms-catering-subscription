package com.mcalvaro.mscatering.infrastructure.api.consolidatedcalendar;

import com.mcalvaro.mscatering.domain.consolidatedcalendar.ConsolidatedCalendar;
import com.mcalvaro.mscatering.domain.consolidatedcalendar.IConsolidatedCalendarRepository;
import com.mcalvaro.mscatering.domain.consolidatedcalendar.entity.ConsolidatedLine;
import com.mcalvaro.mscatering.domain.subscription.enums.ServiceType;
import com.mcalvaro.mscatering.domain.subscription.vo.DeliveryAddress;
import com.mcalvaro.mscatering.domain.subscription.vo.TimeWindow;
import com.mcalvaro.mscatering.infrastructure.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Integration tests for GET /api/consolidated-calendars?date={date}. */
class GetConsolidatedCalendarByDateApiIT extends BaseIntegrationTest {

    private static final String URL = "/api/consolidated-calendars";

    @Autowired
    private IConsolidatedCalendarRepository calendarRepository;

    private UUID calendarId;
    private UUID subscriptionId;
    private UUID patientId;
    private UUID dietPlanId;
    private LocalDate calendarDate;

    @BeforeEach
    void setUpCalendar() {
        calendarId = UUID.randomUUID();
        subscriptionId = UUID.randomUUID();
        patientId = UUID.randomUUID();
        dietPlanId = UUID.randomUUID();
        calendarDate = LocalDate.now().plusDays(10);
        calendarRepository.save(buildClosedCalendar());
    }

    @Nested
    @DisplayName("Happy Path")
    class HappyPath {

        @Test
        @DisplayName("Should return 200 OK with the closed calendar and its delivery line when the date exists")
        void shouldReturn200WithCalendarDetailsWhenDateExists() throws Exception {
            // Arrange
            String requestedDate = calendarDate.toString();

            // Act & Assert
            mockMvc.perform(get(URL).param("date", requestedDate))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(calendarId.toString()))
                    .andExpect(jsonPath("$.date").value(requestedDate))
                    .andExpect(jsonPath("$.status").value("CLOSED"))
                    .andExpect(jsonPath("$.totalDeliveries").value(1))
                    .andExpect(jsonPath("$.closedBy").value("daily-operator"))
                    .andExpect(jsonPath("$.lines.length()").value(1))
                    .andExpect(jsonPath("$.lines[0].subscriptionId").value(subscriptionId.toString()))
                    .andExpect(jsonPath("$.lines[0].patientId").value(patientId.toString()))
                    .andExpect(jsonPath("$.lines[0].dietPlanId").value(dietPlanId.toString()))
                    .andExpect(jsonPath("$.lines[0].serviceType").value("LUNCH"));
        }
    }

    @Nested
    @DisplayName("Domain Rule Violations")
    class DomainRuleViolations {

        @Test
        @DisplayName("Should return 404 NOT_FOUND when no calendar exists for the requested date")
        void shouldReturn404WhenCalendarDoesNotExistForRequestedDate() throws Exception {
            // Arrange
            String requestedDate = calendarDate.plusDays(1).toString();

            // Act & Assert
            mockMvc.perform(get(URL).param("date", requestedDate))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("Malformed Payload")
    class MalformedPayload {

        @Test
        @DisplayName("Should return 500 INTERNAL_SERVER_ERROR when the date query parameter has an invalid format")
        void shouldReturn500WhenDateQueryParameterHasInvalidFormat() throws Exception {
            // Arrange
            String invalidDate = "not-a-date";

            // Act & Assert
            mockMvc.perform(get(URL).param("date", invalidDate))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"));
        }
    }

    private ConsolidatedCalendar buildClosedCalendar() {
        ConsolidatedCalendar calendar = ConsolidatedCalendar.create(calendarId, calendarDate);
        calendar.addLine(new ConsolidatedLine(
                UUID.randomUUID(),
                calendarId,
                subscriptionId,
                patientId,
                dietPlanId,
                ServiceType.LUNCH,
                new DeliveryAddress("Main Street", "123", "Madrid", "Reception", 40.4168, -3.7038,
                        "+34123456789"),
                new TimeWindow(LocalTime.of(12, 0), LocalTime.of(14, 0)),
                "Ring the doorbell"));
        calendar.close("daily-operator");
        return calendar;
    }
}
