package com.placementmanagement.api.placement;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;
import com.placementmanagement.api.auth.entity.UserAccount;
import com.placementmanagement.api.auth.entity.UserRole;
import com.placementmanagement.api.auth.repository.UserAccountRepository;
import com.placementmanagement.api.placement.service.PlacementAuthorizationService;

class PlacementAuthorizationServiceTest {
    @Test
    void rejectsStudentsFromStaffOperations() {
        UserAccountRepository users = mock(UserAccountRepository.class);
        Authentication authentication = mock(Authentication.class);
        UserAccount user = mock(UserAccount.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("7");
        when(users.findById(7L)).thenReturn(Optional.of(user));
        when(user.isEnabled()).thenReturn(true);
        when(user.getRole()).thenReturn(UserRole.STUDENT);

        assertThrows(ResponseStatusException.class,
                () -> new PlacementAuthorizationService(users).staff(authentication));
    }
}
