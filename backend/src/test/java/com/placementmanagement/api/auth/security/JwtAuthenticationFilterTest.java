package com.placementmanagement.api.auth.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.security.core.context.SecurityContextHolder;

import com.placementmanagement.api.auth.entity.UserAccount;
import com.placementmanagement.api.auth.entity.UserRole;
import com.placementmanagement.api.auth.repository.UserAccountRepository;

class JwtAuthenticationFilterTest {
    private final JwtService jwtService = Mockito.mock(JwtService.class);
    private final UserAccountRepository users = Mockito.mock(UserAccountRepository.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, users);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void grantsTheCurrentDatabaseRoleInsteadOfTheTokenRoleClaim() throws Exception {
        UserAccount user = Mockito.mock(UserAccount.class);
        when(jwtService.userId("valid-token")).thenReturn("7");
        when(jwtService.role("valid-token")).thenReturn("STUDENT");
        when(users.findById(7L)).thenReturn(Optional.of(user));
        when(user.isEnabled()).thenReturn(true);
        when(user.getRole()).thenReturn(UserRole.UTPO);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertTrue(SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_UTPO")));
        verify(jwtService).userId("valid-token");
        verify(jwtService, never()).role("valid-token");
    }

    @Test
    void inactiveAccountsRemainUnauthenticated() throws Exception {
        UserAccount user = Mockito.mock(UserAccount.class);
        when(jwtService.userId("valid-token")).thenReturn("7");
        when(users.findById(7L)).thenReturn(Optional.of(user));
        when(user.isEnabled()).thenReturn(false);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertEquals(null, SecurityContextHolder.getContext().getAuthentication());
    }
}
