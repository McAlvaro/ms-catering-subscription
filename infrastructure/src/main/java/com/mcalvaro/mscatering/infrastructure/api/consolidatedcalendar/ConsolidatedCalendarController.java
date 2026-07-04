package com.mcalvaro.mscatering.infrastructure.api.consolidatedcalendar;

import an.awesome.pipelinr.Pipeline;
import com.mcalvaro.mscatering.application.consolidatedcalendar.CloseConsolidatedCalendar.CloseConsolidatedCalendarCommand;
import com.mcalvaro.mscatering.application.consolidatedcalendar.GetConsolidatedCalendarByDate.ConsolidatedCalendarDto;
import com.mcalvaro.mscatering.application.consolidatedcalendar.GetConsolidatedCalendarByDate.GetConsolidatedCalendarByDateQuery;
import com.mcalvaro.mscatering.infrastructure.api.consolidatedcalendar.dto.CloseConsolidatedCalendarRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/consolidated-calendars")
public class ConsolidatedCalendarController {

    private final Pipeline pipeline;

    public ConsolidatedCalendarController(Pipeline pipeline) {
        this.pipeline = pipeline;
    }

    @PostMapping("/close")
    public ResponseEntity<UUID> closeCalendar(@RequestBody CloseConsolidatedCalendarRequest request) {
        CloseConsolidatedCalendarCommand command = new CloseConsolidatedCalendarCommand(
                request.date(),
                request.closedBy()
        );
        UUID calendarId = pipeline.send(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(calendarId);
    }

    @GetMapping
    public ResponseEntity<ConsolidatedCalendarDto> getByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        ConsolidatedCalendarDto dto = pipeline.send(new GetConsolidatedCalendarByDateQuery(date));
        if (dto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(dto);
    }
}
