package com.mcalvaro.mscatering.infrastructure.config;

import an.awesome.pipelinr.Command;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.ObjectProvider;

import com.mcalvaro.mscatering.application.abstractions.DomainEventDispatcher;

/**
 * Middleware para Pipelinr que intercepta la ejecución de todos los
 * comandos y consultas para envolverlos en una transacción de base de datos.
 * <p>
 * Gracias a esta clase, no es necesario anotar cada controlador ni cada
 * CommandHandler con @Transactional. Si ocurre una excepción dentro
 * de un Handler, Spring automáticamente hará rollback de los cambios JPA.
 */
@Component
public class TransactionalMiddleware implements Command.Middleware {

    private final ObjectProvider<DomainEventDispatcher> dispatcherProvider;

    public TransactionalMiddleware(ObjectProvider<DomainEventDispatcher> dispatcherProvider) {
        this.dispatcherProvider = dispatcherProvider;
    }

    @Override
    @Transactional
    public <R, C extends Command<R>> R invoke(C command, Next<R> next) {

        R result = next.invoke();

        dispatcherProvider.getObject().dispatch();

        return result;
    }
}
