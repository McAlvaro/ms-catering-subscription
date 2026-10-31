package com.mcalvaro.mscatering.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.netflix.discovery.EurekaClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.eureka.EurekaDiscoveryClient;
import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaAutoServiceRegistration;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

class TestProfileEurekaIsolationTest extends BaseIntegrationTest {

    @Autowired
    private Environment environment;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void testProfileDoesNotCreateEurekaClientsOrRegistration() throws Exception {
        assertEquals("false", environment.getProperty("eureka.client.enabled"));
        assertTrue(applicationContext.getBeansOfType(EurekaClient.class).isEmpty());
        assertTrue(applicationContext.getBeansOfType(EurekaDiscoveryClient.class).isEmpty());
        assertTrue(applicationContext.getBeansOfType(EurekaAutoServiceRegistration.class).isEmpty());

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
