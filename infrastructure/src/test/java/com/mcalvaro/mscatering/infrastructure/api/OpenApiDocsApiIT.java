package com.mcalvaro.mscatering.infrastructure.api;

import com.mcalvaro.mscatering.infrastructure.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OpenApiDocsApiIT extends BaseIntegrationTest {

    @Test
    void exposesOnlyTheApprovedAnonymousOpenApiContract() throws Exception {
        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.paths['/api/subscriptions']").exists())
                .andExpect(jsonPath("$.paths['/api/subscriptions/{id}']").exists())
                .andExpect(jsonPath("$.paths['/api/consolidated-calendars']").exists())
                .andExpect(jsonPath("$.paths['/api/consolidated-calendars/close']").exists())
                .andExpect(jsonPath("$.paths['/api/patients']").doesNotExist())
                .andExpect(jsonPath("$.paths['/actuator/health']").doesNotExist());
    }

    @Test
    void doesNotHostSwaggerUi() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().isNotFound());
    }
}
