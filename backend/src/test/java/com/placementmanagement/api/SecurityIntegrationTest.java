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
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import com.placementmanagement.api.auth.dto.AuthDtos.AuthResponse;
import com.placementmanagement.api.auth.entity.Department;
import com.placementmanagement.api.auth.entity.Student;
import com.placementmanagement.api.auth.entity.UserAccount;
import com.placementmanagement.api.auth.entity.UserRole;
import com.placementmanagement.api.auth.repository.UserAccountRepository;
import com.placementmanagement.api.auth.security.JwtService;
import com.placementmanagement.api.auth.service.AuthService;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserAccountRepository users;

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

    @Test
    void authenticationErrorsRemainUnauthorizedAfterErrorDispatch() throws Exception {
        when(authService.login(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enrollmentNumber\":\"ENR-42\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidJwtCannotAccessProtectedEndpoints() throws Exception {
        mockMvc.perform(get("/api/protected-resource")
                        .header("Authorization", "Bearer not-a-valid-jwt"))
                .andExpect(status().isForbidden());
    }

    @Test
    void currentUserEndpointReturnsSafeProfileForAuthenticatedRequest() throws Exception {
        UserAccount user = org.mockito.Mockito.mock(UserAccount.class);
        Student student = org.mockito.Mockito.mock(Student.class);
        Department department = org.mockito.Mockito.mock(Department.class);
        when(jwtService.userId("valid-token")).thenReturn("42");
        when(jwtService.role("valid-token")).thenReturn("STUDENT");
        when(users.findById(42L)).thenReturn(java.util.Optional.of(user));
        when(user.isEnabled()).thenReturn(true);
        when(user.getId()).thenReturn(42L);
        when(user.getFullName()).thenReturn("Test Student");
        when(user.getUniversityEmail()).thenReturn("student@example.edu");
        when(user.getRole()).thenReturn(UserRole.STUDENT);
        when(user.getStudent()).thenReturn(student);
        when(student.getEnrollmentNumber()).thenReturn("ENR-42");
        when(student.getDepartment()).thenReturn(department);
        when(department.getId()).thenReturn(7L);
        when(department.getCode()).thenReturn("CSE");
        when(department.getName()).thenReturn("Computer Science");

        mockMvc.perform(get("/api/me").header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(42))
                .andExpect(jsonPath("$.enrollmentNumber").value("ENR-42"))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.department.code").value("CSE"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void currentUserEndpointRejectsAnonymousRequestsWithUnauthorized() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void currentUserEndpointRejectsInvalidTokensWithUnauthorized() throws Exception {
        mockMvc.perform(get("/api/me").header("Authorization", "Bearer not-a-valid-jwt"))
                .andExpect(status().isUnauthorized());
    }
}
