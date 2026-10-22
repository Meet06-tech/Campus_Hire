package com.placementmanagement.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.placementmanagement.api.SecurityConfig;

class PasswordHashCliTest {
    @Test
    void usesTheApplicationEncoderAndClearsInputCharacters() {
        char[] input = new char[] {'t', 'e', 's', 't', '-', 'o', 'n', 'l', 'y'};

        String hash = PasswordHashCli.hash(input);

        assertTrue(hash.startsWith("$2"));
        assertTrue(SecurityConfig.createPasswordEncoder().matches("test-only", hash));
        for (char value : input) assertEquals('\0', value);
    }

    @Test
    void rejectsEmptyInputWithoutCreatingAHash() {
        char[] input = new char[0];

        assertThrows(IllegalArgumentException.class, () -> PasswordHashCli.hash(input));
    }
}
