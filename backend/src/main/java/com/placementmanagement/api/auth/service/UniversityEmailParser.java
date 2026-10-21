package com.placementmanagement.api.auth.service;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UniversityEmailParser {
    private static final String UNIVERSITY_DOMAIN = "charusat.edu.in";
    private static final Pattern LOCAL_PART = Pattern.compile("^(\\d+)([A-Za-z]+)(\\d+)$");

    public String extractCode(String email) {
        if (email == null || email.isBlank()) {
            throw invalidEmail();
        }

        String normalized = email.trim().toLowerCase(Locale.ROOT);
        int at = normalized.indexOf('@');
        if (at <= 0 || at != normalized.lastIndexOf('@')
                || !normalized.substring(at + 1).equals(UNIVERSITY_DOMAIN)) {
            throw invalidEmail();
        }

        Matcher matcher = LOCAL_PART.matcher(normalized.substring(0, at));
        if (!matcher.matches()) {
            throw invalidEmail();
        }
        return matcher.group(2).toUpperCase(Locale.ROOT);
    }

    private ResponseStatusException invalidEmail() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "University email must match the approved format and domain");
    }
}
