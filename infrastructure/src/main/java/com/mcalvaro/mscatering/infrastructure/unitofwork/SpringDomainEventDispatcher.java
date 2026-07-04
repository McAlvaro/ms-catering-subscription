package com.mcalvaro.mscatering.infrastructure.unitofwork;

import an.awesome.pipelinr.Pipeline;
import com.mcalvaro.mscatering.application.abstractions.DomainEventNotification;
import com.mcalvaro.mscatering.application.abstractions.DomainEventDispatcher;
import com.mcalvaro.mscatering.domain.core.AggregateRoot;
import com.mcalvaro.mscatering.domain.core.DomainEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementación de {@link DomainEventDispatcher} respaldada por Spring.
 * <p>
 * Su ciclo de vida es @RequestScope para garantizar que cada petición HTTP
 * tenga su propia instancia y la lista de agregados no se comparta entre hilos.
 */
@Component
@RequestScope
public class SpringDomainEventDispatcher implements DomainEventDispatcher {

    private final Pipeline pipeline;
    private final List<AggregateRoot> aggregates = new ArrayList<>();

    public SpringDomainEventDispatcher(Pipeline pipeline) {
        this.pipeline = pipeline;
    }

    @Override
    public void register(AggregateRoot aggregate) {
        if (!aggregates.contains(aggregate)) {
            aggregates.add(aggregate);
        }
    }

    @Override
    public void dispatch() {
        for (AggregateRoot aggregate : aggregates) {
            for (DomainEvent event : aggregate.getDomainEvents()) {
                pipeline.send(new DomainEventNotification(event));
            }
            aggregate.clearDomainEvents();
        }

        aggregates.clear();
    }
}
