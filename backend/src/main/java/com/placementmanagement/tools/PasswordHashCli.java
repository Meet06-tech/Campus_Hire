package com.placementmanagement.tools;

import java.awt.GraphicsEnvironment;
import java.util.Arrays;

import javax.swing.JOptionPane;
import javax.swing.JPasswordField;

import com.placementmanagement.api.SecurityConfig;

/**
 * Local-only password hash helper. It is not a Spring component or an HTTP endpoint.
 */
public final class PasswordHashCli {
    private PasswordHashCli() {}

    public static void main(String[] args) {
        char[] password = readPassword();
        try {
            System.out.println(hash(password));
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    static String hash(char[] password) {
        if (password == null || password.length == 0) {
            if (password != null) Arrays.fill(password, '\0');
            throw new IllegalArgumentException("Password must not be empty");
        }
        try {
            return SecurityConfig.createPasswordEncoder().encode(new String(password));
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private static char[] readPassword() {
        if (System.console() != null) {
            char[] password = System.console().readPassword("Password (input hidden): ");
            if (password == null) throw new IllegalStateException("No password was entered");
            return password;
        }
        if (GraphicsEnvironment.isHeadless()) {
            throw new IllegalStateException(
                    "No interactive hidden-input console is available; run this class from a terminal or IntelliJ desktop session");
        }
        JPasswordField field = new JPasswordField();
        int result = JOptionPane.showConfirmDialog(
                null, field, "CampusHire password", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) throw new IllegalStateException("Password entry was cancelled");
        return field.getPassword();
    }
}
