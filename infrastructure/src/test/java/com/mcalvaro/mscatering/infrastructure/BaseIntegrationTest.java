package com.mcalvaro.mscatering.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Clase base para pruebas de integración del microservicio.
 * <p>
 * Inicia el contexto completo de Spring Boot con H2 en memoria (modo MySQL)
 * y migraciones Liquibase aplicadas. Proporciona {@link MockMvc} y
 * {@link ObjectMapper}
 * para probar los endpoints REST de extremo a extremo.
 */
@SpringBootTest(classes = MsCateringApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired(required = false)
    protected ObjectMapper objectMapper;

    @BeforeEach
    void setUpBaseIntegrationTest() {
        if (objectMapper == null) {
            objectMapper = new ObjectMapper()
                    .registerModule(new JavaTimeModule())
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        }
    }
}
