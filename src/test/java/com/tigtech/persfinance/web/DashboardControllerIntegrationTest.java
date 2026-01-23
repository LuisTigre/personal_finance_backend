package com.tigtech.persfinance.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class DashboardControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser // Simulates an authenticated user
    public void testGetOverview_HappyPath() throws Exception {
        mockMvc.perform(get("/api/dashboards/overview"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    public void testGetBudgets_HappyPath() throws Exception {
        mockMvc.perform(get("/api/dashboards/budgets"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    public void testGetAnalytics_HappyPath() throws Exception {
        mockMvc.perform(get("/api/dashboards/analytics"))
                .andExpect(status().isOk());
    }
}
