package com.mcalvaro.mscatering.infrastructure.config;

import an.awesome.pipelinr.Command;
import an.awesome.pipelinr.CommandHandlers;
import an.awesome.pipelinr.Pipeline;
import an.awesome.pipelinr.Pipelinr;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configura el bus de comandos/queries de Pipelinr.
 * <p>
 * Pipelinr descubre automáticamente todos los beans que implementan
 * {@link Command.Handler} y los registra como manejadores disponibles
 * para el {@link Pipeline}. El controlador REST inyecta {@code Pipeline}
 * y despacha comandos/queries sin conocer la implementación concreta
 * del handler (Mediator Pattern).
 */
@Configuration
public class PipelinrConfig {

    /**
     * Expone el Pipeline de Pipelinr como un bean singleton.
     * <p>
     * {@code ObjectProvider<Command.Handler>} recolecta todos los handlers
     * registrados como beans por {@link ApplicationBeansConfig}.
     */
    @Bean
    @SuppressWarnings("rawtypes")
    Pipeline pipeline(ObjectProvider<Command.Handler> handlers) {
        return new Pipelinr()
                .with((CommandHandlers) handlers::stream);
    }
}
