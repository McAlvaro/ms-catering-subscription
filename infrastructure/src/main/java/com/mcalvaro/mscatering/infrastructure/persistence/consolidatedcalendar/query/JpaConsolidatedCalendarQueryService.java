package com.mcalvaro.mscatering.infrastructure.persistence.consolidatedcalendar.query;

import com.mcalvaro.mscatering.application.consolidatedcalendar.GetConsolidatedCalendarByDate.ConsolidatedCalendarDto;
import com.mcalvaro.mscatering.application.consolidatedcalendar.GetConsolidatedCalendarByDate.ConsolidatedLineDto;
import com.mcalvaro.mscatering.application.consolidatedcalendar.GetConsolidatedCalendarByDate.IConsolidatedCalendarQueryService;
import com.mcalvaro.mscatering.infrastructure.persistence.consolidatedcalendar.entity.ConsolidatedCalendarJpaEntity;
import com.mcalvaro.mscatering.infrastructure.persistence.consolidatedcalendar.repository.SpringDataConsolidatedCalendarRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class JpaConsolidatedCalendarQueryService implements IConsolidatedCalendarQueryService {

    private final SpringDataConsolidatedCalendarRepository springRepository;

    public JpaConsolidatedCalendarQueryService(SpringDataConsolidatedCalendarRepository springRepository) {
        this.springRepository = springRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ConsolidatedCalendarDto> getByDate(LocalDate date) {
        return springRepository.findByDate(date).map(this::mapToDetailsDto);
    }

    private ConsolidatedCalendarDto mapToDetailsDto(ConsolidatedCalendarJpaEntity entity) {
        List<ConsolidatedLineDto> lines = entity.getLines().stream()
                .map(line -> new ConsolidatedLineDto(
                        line.getId(),
                        line.getSubscriptionId(),
                        line.getPatientId(),
                        line.getDietPlanId(),
                        line.getServiceType(),
                        line.getAddressStreet(),
                        line.getAddressNumber(),
                        line.getAddressCity(),
                        line.getAddressReference(),
                        line.getAddressLatitude() != null ? line.getAddressLatitude() : 0.0,
                        line.getAddressLongitude() != null ? line.getAddressLongitude() : 0.0,
                        line.getAddressPhone(),
                        LocalTime.parse(line.getTimeStart()),
                        LocalTime.parse(line.getTimeEnd()),
                        line.getInstructions()
                ))
                .collect(Collectors.toList());

        return new ConsolidatedCalendarDto(
                entity.getId(),
                entity.getDate(),
                entity.getStatus(),
                entity.getTotalDeliveries(),
                entity.getClosedAt(),
                entity.getClosedBy(),
                lines
        );
    }
}
