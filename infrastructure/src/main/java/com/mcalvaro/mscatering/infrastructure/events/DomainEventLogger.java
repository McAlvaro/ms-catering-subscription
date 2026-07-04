package com.mcalvaro.mscatering.infrastructure.events;

import an.awesome.pipelinr.Notification;
import com.mcalvaro.mscatering.application.abstractions.DomainEventNotification;
import org.springframework.stereotype.Component;

@Component
public class DomainEventLogger implements Notification.Handler<DomainEventNotification> {

    @Override
    public void handle(DomainEventNotification notification) {
        // En una aplicación real aquí podríamos publicarlo a un bus de eventos (RabbitMQ/Kafka)
        System.out.println("======================================================");
        System.out.println(" [DOMAIN EVENT DISPATCHED] -> " + notification.domainEvent().getClass().getSimpleName());
        System.out.println("   Data: " + notification.domainEvent().toString());
        System.out.println("======================================================");
    }
}
