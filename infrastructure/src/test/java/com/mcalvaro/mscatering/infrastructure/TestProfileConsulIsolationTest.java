package com.mcalvaro.mscatering.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ecwid.consul.v1.ConsulClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.consul.discovery.ConsulDiscoveryClient;
import org.springframework.cloud.consul.serviceregistry.ConsulAutoServiceRegistration;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

class TestProfileConsulIsolationTest extends BaseIntegrationTest {

    @Autowired
    private Environment environment;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void testProfileDoesNotCreateConsulClientsOrRegistration() throws Exception {
        assertEquals("false", environment.getProperty("spring.cloud.consul.enabled"));
        assertTrue(applicationContext.getBeansOfType(ConsulClient.class).isEmpty());
        assertTrue(applicationContext.getBeansOfType(ConsulDiscoveryClient.class).isEmpty());
        assertTrue(applicationContext.getBeansOfType(ConsulAutoServiceRegistration.class).isEmpty());

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
