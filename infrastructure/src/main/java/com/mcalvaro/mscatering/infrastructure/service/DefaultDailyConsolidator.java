package com.mcalvaro.mscatering.infrastructure.service;

import com.mcalvaro.mscatering.domain.consolidatedcalendar.ConsolidatedCalendar;
import com.mcalvaro.mscatering.domain.consolidatedcalendar.entity.ConsolidatedLine;
import com.mcalvaro.mscatering.domain.consolidatedcalendar.service.DailyConsolidator;
import com.mcalvaro.mscatering.domain.subscription.ISubscriptionRepository;
import com.mcalvaro.mscatering.domain.subscription.Subscription;
import com.mcalvaro.mscatering.domain.subscription.entity.DeliveryDay;
import com.mcalvaro.mscatering.domain.subscription.enums.DeliveryDayStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Component
public class DefaultDailyConsolidator implements DailyConsolidator {

    private final ISubscriptionRepository subscriptionRepository;

    public DefaultDailyConsolidator(ISubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    @Override
    public ConsolidatedCalendar consolidateForDate(LocalDate deliveryDate) {
        // 1. Crear el Calendario Consolidado para la fecha indicada (estado ABIERTO)
        ConsolidatedCalendar calendar = ConsolidatedCalendar.create(UUID.randomUUID(), deliveryDate);

        // 2. Obtener todas las suscripciones activas
        List<Subscription> activeSubscriptions = subscriptionRepository.findActiveSubscriptions();

        // 3. Cruzar suscripciones y recopilar entregas programadas
        for (Subscription subscription : activeSubscriptions) {
            List<DeliveryDay> days = subscription.getDeliveryCalendar().getDaysForDate(deliveryDate);

            for (DeliveryDay day : days) {
                if (day.getStatus() == DeliveryDayStatus.SCHEDULED) {
                    // Generar la línea consolidada
                    ConsolidatedLine line = new ConsolidatedLine(
                            UUID.randomUUID(),
                            calendar.getId(),
                            subscription.getId(),
                            subscription.getPatientId(),
                            subscription.getDietPlanId(),
                            subscription.getContract().serviceType(),
                            day.getAddress(),
                            day.getTimeWindow(),
                            day.getInstructions());

                    calendar.addLine(line);

                    // Cambiar el estado del día de entrega a CONSOLIDADO
                    day.markAsConsolidated();

                    // Guardar los cambios en el agregado de Suscripción
                    subscriptionRepository.save(subscription);
                }
            }
        }

        return calendar;
    }
}
