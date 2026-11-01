package com.placementmanagement.tools;

import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;
import javax.swing.JPasswordField;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import com.placementmanagement.api.PlacementManagementApplication;

/**
 * Manually invoked, local-only staff provisioning entry point.
 * It is deliberately not a Spring bean or a CommandLineRunner.
 */
public final class StaffProvisioningCli {
    private static final String CONFIRMATION = "PROVISION EXACTLY ONE UTPO AND ONE DTPO";
    private static final List<String> REQUIRED_WRITE_TABLES = List.of("users");

    private StaffProvisioningCli() {
    }

    public static void main(String[] args) {
        validateArguments(args);
        ensureInteractiveInputAvailable(System.console() != null, GraphicsEnvironment.isHeadless());

        ConfigurableApplicationContext context = new SpringApplicationBuilder(PlacementManagementApplication.class)
                .web(WebApplicationType.NONE)
                .properties("spring.main.banner-mode=off")
                .run();
        try {
            verifyOwner(context.getBean(JdbcTemplate.class));
            StaffProvisioningRequest request = readRequest();
            try (request) {
                if (!isConfirmed(readText("Type exactly " + CONFIRMATION + " to continue"))) {
                    throw new IllegalStateException("Provisioning was not confirmed; no accounts were created");
                }
                context.getBean(StaffProvisioningService.class).provision(request);
                System.out.println("Staff provisioning completed: one UTPO and one DTPO account created.");
            }
        } finally {
            context.close();
        }
    }

    static void validateArguments(String[] args) {
        if (args == null || args.length != 0) {
            throw new IllegalArgumentException("Do not pass provisioning values as command-line arguments");
        }
    }

    static boolean isConfirmed(String input) {
        return CONFIRMATION.equals(input);
    }

    static void ensureInteractiveInputAvailable(boolean consoleAvailable, boolean headless) {
        if (!consoleAvailable && headless) {
            throw new IllegalStateException(
                    "Hidden interactive input is unavailable. Run StaffProvisioningCli from an IntelliJ desktop session "
                            + "with GUI dialogs enabled, or from a terminal with a real console.");
        }
    }

    static void verifyOwner(JdbcTemplate jdbc) {
        String expectedOwner = System.getenv("PROVISIONING_DB_OWNER");
        if (expectedOwner == null || expectedOwner.isBlank()) {
            throw new IllegalStateException("Set PROVISIONING_DB_OWNER to the approved database-owner role before running");
        }
        Map<String, Object> database = jdbc.queryForMap("""
                SELECT current_database() AS current_database,
                       current_user AS current_role,
                       pg_get_userbyid(datdba) AS database_owner
                FROM pg_database
                WHERE datname = current_database()
                """);
        Set<String> unownedTables = new LinkedHashSet<>();
        for (String table : REQUIRED_WRITE_TABLES) {
            Integer owned = jdbc.queryForObject("""
                    SELECT COUNT(*)
                    FROM pg_class c
                    JOIN pg_namespace n ON n.oid = c.relnamespace
                    WHERE n.nspname = 'public'
                      AND c.relname = ?
                      AND c.relkind IN ('r', 'p')
                      AND pg_get_userbyid(c.relowner) = current_user
                    """, Integer.class, table);
            if (!Integer.valueOf(1).equals(owned)) {
                unownedTables.add(table);
            }
        }
        validateOwnership(String.valueOf(database.get("current_database")),
                String.valueOf(database.get("current_role")), expectedOwner,
                String.valueOf(database.get("database_owner")), unownedTables);
    }

    static void validateOwnership(String currentDatabase, String currentRole, String expectedOwner,
            String databaseOwner, Set<String> unownedTables) {
        if (!"placement_management".equals(currentDatabase)) {
            throw new IllegalStateException("The database connection is not placement_management");
        }
        if (!expectedOwner.equals(currentRole)) {
            throw new IllegalStateException("The effective database role is not the configured provisioning owner");
        }
        if (!expectedOwner.equals(databaseOwner)) {
            throw new IllegalStateException("The configured provisioning role does not own placement_management");
        }
        if (!unownedTables.isEmpty()) {
            throw new IllegalStateException("The provisioning role does not own required table(s): " + unownedTables);
        }
    }

    private static StaffProvisioningRequest readRequest() {
        char[] utpoPassword = null;
        char[] dtpoPassword = null;
        try {
            String utpoName = readText("U.TPO full name");
            String utpoEmail = readText("U.TPO university email");
            utpoPassword = readPassword("U.TPO password");
            String dtpoName = readText("D.TPO full name");
            String dtpoEmail = readText("D.TPO university email");
            dtpoPassword = readPassword("D.TPO password");
            return new StaffProvisioningRequest(utpoName, utpoEmail, utpoPassword,
                    dtpoName, dtpoEmail, dtpoPassword);
        } catch (RuntimeException exception) {
            clear(utpoPassword);
            clear(dtpoPassword);
            throw exception;
        }
    }

    private static String readText(String prompt) {
        if (System.console() != null) {
            String value = System.console().readLine("%s: ", prompt);
            if (value != null) return value;
        } else if (!GraphicsEnvironment.isHeadless()) {
            String value = JOptionPane.showInputDialog(null, prompt);
            if (value != null) return value;
        }
        throw new IllegalStateException("Interactive input was cancelled or is unavailable");
    }

    private static char[] readPassword(String prompt) {
        if (System.console() != null) {
            char[] value = System.console().readPassword("%s (input hidden): ", prompt);
            if (value != null) return value;
        } else if (!GraphicsEnvironment.isHeadless()) {
            JPasswordField field = new JPasswordField();
            int result = JOptionPane.showConfirmDialog(null, field, prompt,
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result == JOptionPane.OK_OPTION) return field.getPassword();
        }
        throw new IllegalStateException("Hidden password input was cancelled or is unavailable");
    }

    private static void clear(char[] value) {
        if (value != null) Arrays.fill(value, '\0');
    }
}
