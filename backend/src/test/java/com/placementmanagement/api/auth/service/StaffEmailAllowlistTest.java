package com.placementmanagement.api.auth.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StaffEmailAllowlistTest {
    @Test
    void matchesApprovedDomainsCaseInsensitively() {
        StaffEmailAllowlist allowlist = new StaffEmailAllowlist("charusat.ac.in, another-approved.edu");

        assertTrue(allowlist.isAllowed("TPO@CHARUSAT.AC.IN"));
        assertTrue(allowlist.isAllowed("tpo@another-approved.edu"));
    }

    @Test
    void rejectsLookalikeAndMalformedDomains() {
        StaffEmailAllowlist allowlist = new StaffEmailAllowlist("charusat.ac.in");

        assertFalse(allowlist.isAllowed("tpo@evilcharusat.ac.in"));
        assertFalse(allowlist.isAllowed("tpo@charusat.ac.in.evil"));
        assertFalse(allowlist.isAllowed("tpo@charusat.ac.in.evil"));
        assertFalse(allowlist.isAllowed("tpo@@charusat.ac.in"));
        assertFalse(allowlist.isAllowed(".@charusat.ac.in"));
        assertFalse(allowlist.isAllowed("t..po@charusat.ac.in"));
    }

    @Test
    void blankConfigurationFailsClosed() {
        StaffEmailAllowlist allowlist = new StaffEmailAllowlist("  ");

        assertFalse(allowlist.isAllowed("tpo@charusat.ac.in"));
    }

    @Test
    void invalidConfigurationFailsClearly() {
        assertThrows(IllegalStateException.class, () -> new StaffEmailAllowlist("charusat.ac.in,not a domain"));
    }
}
