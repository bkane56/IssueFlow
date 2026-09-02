package com.issueflow.controller;

import com.issueflow.config.TimeConfig;
import com.issueflow.dto.response.DashboardResponse;
import com.issueflow.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
@Import(TimeConfig.class)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @Test
    void returnsDashboardStatistics() throws Exception {
        when(dashboardService.getDashboard()).thenReturn(new DashboardResponse(12, 2, 5, 18));

        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.open").value(12))
                .andExpect(jsonPath("$.critical").value(2))
                .andExpect(jsonPath("$.inProgress").value(5))
                .andExpect(jsonPath("$.resolved").value(18));
    }
}
