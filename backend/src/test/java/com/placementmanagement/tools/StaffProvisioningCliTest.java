package com.placementmanagement.tools;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Service;

import com.placementmanagement.api.PlacementManagementApplication;

class StaffProvisioningCliTest {
    @Test
    void acceptsExpectedDatabaseAndActualOwnerMetadata() {
        assertDoesNotThrow(() -> StaffProvisioningCli.validateOwnership(
                "placement_management", "meetbhoi", "meetbhoi", "meetbhoi", Set.of()));
    }

    @Test
    void rejectsRoleThatOnlyMatchesConfiguredNameButDoesNotOwnDatabase() {
        assertThrows(IllegalStateException.class, () -> StaffProvisioningCli.validateOwnership(
                "placement_management", "placement_app", "placement_app", "meetbhoi", Set.of()));
    }

    @Test
    void rejectsMissingRequiredTableOwnership() {
        assertThrows(IllegalStateException.class, () -> StaffProvisioningCli.validateOwnership(
                "placement_management", "meetbhoi", "meetbhoi", "meetbhoi", Set.of("users")));
    }

    @Test
    void rejectsUnexpectedDatabase() {
        assertThrows(IllegalStateException.class, () -> StaffProvisioningCli.validateOwnership(
                "other_database", "meetbhoi", "meetbhoi", "meetbhoi", Set.of()));
    }

    @Test
    void rejectsCommandLineValuesAndRequiresExactConfirmation() {
        assertThrows(IllegalArgumentException.class,
                () -> StaffProvisioningCli.validateArguments(new String[] {"email"}));
        assertThrows(IllegalArgumentException.class, () -> StaffProvisioningCli.validateArguments(null));
        assertDoesNotThrow(() -> StaffProvisioningCli.validateArguments(new String[0]));
        assertTrue(StaffProvisioningCli.isConfirmed("PROVISION EXACTLY ONE UTPO AND ONE DTPO"));
        assertFalse(StaffProvisioningCli.isConfirmed("PROVISION ONE ACCOUNT"));
    }

    @Test
    void rejectsHeadlessLaunchBeforeCollectingInput() {
        assertThrows(IllegalStateException.class,
                () -> StaffProvisioningCli.ensureInteractiveInputAvailable(false, true));
        assertDoesNotThrow(() -> StaffProvisioningCli.ensureInteractiveInputAvailable(true, true));
        assertDoesNotThrow(() -> StaffProvisioningCli.ensureInteractiveInputAvailable(false, false));
    }

    @Test
    void applicationScanIncludesTransactionalProvisioningService() {
        SpringBootApplication application = PlacementManagementApplication.class
                .getAnnotation(SpringBootApplication.class);

        assertTrue(java.util.Arrays.asList(application.scanBasePackages())
                .contains("com.placementmanagement.tools"));
        assertTrue(StaffProvisioningService.class.isAnnotationPresent(Service.class));
    }
}
