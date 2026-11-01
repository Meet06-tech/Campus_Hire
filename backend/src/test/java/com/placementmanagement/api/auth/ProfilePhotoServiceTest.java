package com.placementmanagement.api.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

import com.placementmanagement.api.auth.entity.UserAccount;
import com.placementmanagement.api.auth.entity.UserRole;
import com.placementmanagement.api.auth.repository.StudentRepository;
import com.placementmanagement.api.auth.repository.UserAccountRepository;
import com.placementmanagement.api.auth.service.CurrentUserService;
import com.placementmanagement.api.auth.service.ProfilePhotoService;
import com.placementmanagement.api.auth.service.ProfilePhotoStorageService;

class ProfilePhotoServiceTest {
    @Test
    void staffCannotUseStudentPhotoOperations() {
        UserAccount user = mock(UserAccount.class);
        when(user.isEnabled()).thenReturn(true);
        when(user.getRole()).thenReturn(UserRole.DTPO);

        UserAccountRepository repository = mock(UserAccountRepository.class);
        when(repository.findById(7L)).thenReturn(Optional.of(user));
        CurrentUserService currentUser = new CurrentUserService(repository, mock(StudentRepository.class));
        ProfilePhotoService service = new ProfilePhotoService(currentUser,
                mock(ProfilePhotoStorageService.class), mock(StudentRepository.class));
        Authentication authentication = new UsernamePasswordAuthenticationToken("7", null,
                java.util.List.of(new SimpleGrantedAuthority("ROLE_DTPO")));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.current(authentication));

        assertEquals(403, exception.getStatusCode().value());
    }

}
