package com.placementmanagement.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.placementmanagement.api.auth.AuthController;
import com.placementmanagement.api.auth.AuthDtos.AuthResponse;
import com.placementmanagement.api.auth.AuthService;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Test
    void validLoginReachesControllerAndReturnsToken() throws Exception {
        AuthResponse response = new AuthResponse("test-token", "42", "Test Student", "ENR-42", "STUDENT");
        when(authService.login(org.mockito.ArgumentMatchers.any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enrollmentNumber\":\"ENR-42\",\"password\":\"correct-password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("test-token"))
                .andExpect(jsonPath("$.role").value("STUDENT"));

        verify(authService).login(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void protectedEndpointsRejectAnonymousRequests() throws Exception {
        mockMvc.perform(get("/api/protected-resource"))
                .andExpect(status().isForbidden());
    }
}
