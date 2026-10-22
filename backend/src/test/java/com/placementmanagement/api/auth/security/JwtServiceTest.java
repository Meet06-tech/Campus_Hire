package com.placementmanagement.api.auth.security;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.placementmanagement.api.auth.entity.UserAccount;
import com.placementmanagement.api.auth.entity.UserRole;

class JwtServiceTest {
    private static final String SECRET = "test-signing-secret-with-at-least-32-chars";

    @Test
    void rejectsTamperedSignature() {
        JwtService service = new JwtService(SECRET, 60_000);
        UserAccount user = user(42L, UserRole.STUDENT);
        String token = service.createToken(user);

        assertThrows(RuntimeException.class, () -> service.userId(token + "tampered"));
    }

    @Test
    void rejectsExpiredToken() {
        JwtService service = new JwtService(SECRET, -1);
        String token = service.createToken(user(42L, UserRole.UTPO));

        assertThrows(RuntimeException.class, () -> service.userId(token));
    }

    @Test
    void readsSignedSubject() {
        JwtService service = new JwtService(SECRET, 60_000);
        String token = service.createToken(user(42L, UserRole.STUDENT));

        assertEquals("42", service.userId(token));
    }

    private UserAccount user(Long id, UserRole role) {
        UserAccount user = mock(UserAccount.class);
        when(user.getId()).thenReturn(id);
        when(user.getRole()).thenReturn(role);
        return user;
    }
}
