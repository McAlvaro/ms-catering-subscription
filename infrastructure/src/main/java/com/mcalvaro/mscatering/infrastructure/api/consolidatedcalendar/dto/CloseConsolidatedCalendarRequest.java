package com.mcalvaro.mscatering.infrastructure.api.consolidatedcalendar.dto;

import java.time.LocalDate;

public record CloseConsolidatedCalendarRequest(
        LocalDate date,
        String closedBy
) {
}
