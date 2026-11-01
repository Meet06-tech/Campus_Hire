package com.placementmanagement.api.auth.service;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Validates provisioned staff email identifiers against an explicit allowlist. */
@Component
public class StaffEmailAllowlist {
    private static final Pattern DOMAIN = Pattern.compile(
            "^[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?(?:\\.[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)+$");
    private static final Pattern LOCAL_PART = Pattern.compile("^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+$");

    private final Set<String> domains;

    public StaffEmailAllowlist(@Value("${app.auth.staff-allowed-email-domains:}") String configuredDomains) {
        this.domains = parse(configuredDomains);
    }

    public boolean isAllowed(String email) {
        if (email == null || email.isBlank() || email.indexOf('@') != email.lastIndexOf('@')) return false;
        int separator = email.lastIndexOf('@');
        String localPart = email.substring(0, separator);
        if (separator <= 0 || separator == email.length() - 1 || email.chars().anyMatch(Character::isWhitespace)
                || !LOCAL_PART.matcher(localPart).matches() || localPart.startsWith(".")
                || localPart.endsWith(".") || localPart.contains("..")) {
            return false;
        }
        String domain = email.substring(separator + 1).toLowerCase(Locale.ROOT);
        return domains.contains(domain);
    }

    /** Compatibility alias used by the existing coordinator-management service. */
    public boolean accepts(String email) {
        return isAllowed(email);
    }

    private Set<String> parse(String configuredDomains) {
        if (configuredDomains == null || configuredDomains.isBlank()) return Set.of();
        Set<String> parsed = new LinkedHashSet<>();
        for (String rawDomain : configuredDomains.split(",", -1)) {
            String domain = rawDomain.trim().toLowerCase(Locale.ROOT);
            if (!DOMAIN.matcher(domain).matches()) {
                throw new IllegalStateException(
                        "STAFF_ALLOWED_EMAIL_DOMAINS contains an invalid domain; use comma-separated domain names");
            }
            parsed.add(domain);
        }
        return Collections.unmodifiableSet(parsed);
    }
}
