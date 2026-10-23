package com.placementmanagement.api.auth.service;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class StaffEmailAllowlist {
    private final Set<String> domains;

    public StaffEmailAllowlist(@Value("${STAFF_ALLOWED_EMAIL_DOMAINS:}") String configuredDomains) {
        this.domains = Arrays.stream(configuredDomains.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean accepts(String email) {
        if (email == null || email.isBlank()) return false;
        int at = email.lastIndexOf('@');
        if (at <= 0 || at == email.length() - 1 || email.indexOf('@') != at) return false;
        String domain = email.substring(at + 1).toLowerCase(Locale.ROOT);
        return domains.contains(domain);
    }
}
