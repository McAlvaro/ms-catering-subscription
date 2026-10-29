package com.mcalvaro.mscatering.application.outbox.translation;

import com.mcalvaro.mscatering.domain.consolidatedcalendar.entity.ConsolidatedDeliverySnapshot;
import com.mcalvaro.mscatering.domain.consolidatedcalendar.entity.ConsolidatedRecipeSnapshot;
import com.mcalvaro.mscatering.domain.consolidatedcalendar.event.ConsolidatedCalendarClosed;
import com.mcalvaro.mscatering.domain.subscription.vo.DeliveryAddress;
import com.mcalvaro.mscatering.integration.contracts.ConsolidatedCalendarClosedIntegrationEvent;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

public final class ConsolidatedCalendarClosedOutboxEventTranslator
        implements OutboxEventTranslator<ConsolidatedCalendarClosed> {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public Class<ConsolidatedCalendarClosed> sourceType() {
        return ConsolidatedCalendarClosed.class;
    }

    @Override
    public ConsolidatedCalendarClosedIntegrationEvent translate(ConsolidatedCalendarClosed source) {
        if (source == null)
            throw new IntegrationEventTranslationException("ConsolidatedCalendarClosed must not be null");
        return new ConsolidatedCalendarClosedIntegrationEvent(source.eventId(), source.occurredAt(),
                source.correlationId(),
                new ConsolidatedCalendarClosedIntegrationEvent.Data(source.calendarId(), source.date(),
                        source.occurredAt(),
                        source.totalDeliveries(), source.deliveries().stream().map(this::delivery).toList()));
    }

    private ConsolidatedCalendarClosedIntegrationEvent.Delivery delivery(ConsolidatedDeliverySnapshot source) {
        return new ConsolidatedCalendarClosedIntegrationEvent.Delivery(source.calendarDayId(), source.subscriptionId(),
                source.patientId(),
                source.patientName(), source.dietPlanId(), source.planDayNumber(), address(source.address()),
                source.recipes().stream().map(this::recipe).toList());
    }

    private ConsolidatedCalendarClosedIntegrationEvent.Address address(DeliveryAddress source) {
        String number = source.number() == null ? "" : source.number();
        return new ConsolidatedCalendarClosedIntegrationEvent.Address(source.addressId(), source.alias(),
                (source.street() + " " + number).trim(), source.reference(), source.city(), source.latitude(),
                source.longitude());
    }

    private ConsolidatedCalendarClosedIntegrationEvent.Recipe recipe(ConsolidatedRecipeSnapshot source) {
        return new ConsolidatedCalendarClosedIntegrationEvent.Recipe(source.recipeId(), source.name(),
                TIME_FORMAT.format(source.time()),
                BigDecimal.valueOf(source.portionGrams()));
    }
}
