package com.mcalvaro.mscatering.infrastructure.config;

import an.awesome.pipelinr.Command;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.ScopeNotActiveException;
import org.springframework.web.context.request.RequestContextHolder;

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

        dispatchDomainEventsIfAvailable();

        return result;
    }

    private void dispatchDomainEventsIfAvailable() {
        if (RequestContextHolder.getRequestAttributes() != null) {
            try {
                DomainEventDispatcher dispatcher = dispatcherProvider.getIfAvailable();
                if (dispatcher != null) {
                    dispatcher.dispatch();
                }
            } catch (ScopeNotActiveException | IllegalStateException ignored) {
                // Request scope is not active or available for current thread
            }
        }
    }
}
