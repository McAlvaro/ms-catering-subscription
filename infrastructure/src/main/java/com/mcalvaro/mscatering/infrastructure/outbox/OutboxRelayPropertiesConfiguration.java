package com.mcalvaro.mscatering.infrastructure.outbox;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Binds relay settings independently from the opt-in relay beans. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(OutboxRelayProperties.class)
public class OutboxRelayPropertiesConfiguration {
}
