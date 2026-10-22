package com.placementmanagement.tools;

/** Operator-supplied values for the one-time local staff provisioning utility. */
public record StaffProvisioningRequest(
        String utpoName,
        String utpoEmail,
        char[] utpoPassword,
        String dtpoName,
        String dtpoEmail,
        char[] dtpoPassword) implements AutoCloseable {

    @Override
    public void close() {
        clear(utpoPassword);
        clear(dtpoPassword);
    }

    private static void clear(char[] value) {
        if (value != null) {
            java.util.Arrays.fill(value, '\0');
        }
    }
}
